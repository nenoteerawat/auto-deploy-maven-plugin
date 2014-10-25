/*******************************************************************************
 *  Maven Auto Deploy Plugin
 *  Copyright (C) 2014 Stefan Lorenz  
 *  https://github.com/sisao/auto-deploy-maven-plugin
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Contributors:  
 *  	Lorenz, Stefan
 *  
 *  Based on 
 *  Ralph Soika's Manik Hot Deploy 
 *  https://github.com/rsoika/manik-hot-deploy
 *  
 * 
 *******************************************************************************/

package de.sisao.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.maven.plugin.logging.Log;

public class Deployer {

	private static String[] IGNORE_DIRECTORIES = { "/src/main/resources/",
			"/src/main/java/", "/src/test/resources/", "/src/test/java/",
			"/target/m2e-wtp/", "/target/maven-archiver/", "/META-INF/",
			"/target/application.xml", "/target/test-classes/",
			"/target/classes/", "/WEB-INF/classes/" };
	private static String[] IGNORE_SUBDIRECTORIES = { "/classes/",
			"/src/main/webapp/" };

	private String hotdeployTarget = "";
	private boolean hotDeployMode = false;
	private boolean explodeArtifact = false;
	private boolean wildflySupport = false;
	private String sourceFilePath = "";
	private String sourceFileName = "";
	private String sourceFilePathAbsolute = "";

	private Log log;

	void deployResource(File resource, String autodeployTarget, Log log) {

		this.log = log;

		String targetFilePath;

		// do not deploy directory resources!
		if (!(resource instanceof File))
			return;

		File file = resource;
		sourceFileName = file.getName();
		sourceFilePath = file.getPath();
		sourceFilePathAbsolute = file.getAbsolutePath();

		// we do not deploy files from the source directories
		// skip source files like /src/main/java/*
		for (String value : IGNORE_DIRECTORIES) {
			if (sourceFilePath.contains(value)) {
				log.info("Skipping resource: " + sourceFilePath
						+ " because it contains: " + value);
				return;
			}
		}

		if ("".equals(hotdeployTarget))
			hotdeployTarget = null;
		if ("".equals(autodeployTarget))
			autodeployTarget = null;

		// check for an missing/invalid confiugration
		if (autodeployTarget == null && hotdeployTarget == null) {
			// no message is needed here!
			return;
		}

		// check if a .ear or .war file should be autodeplyed....
		if ((sourceFileName.endsWith(".ear") || sourceFileName.endsWith(".war"))) {
			// verify if target autodeploy folder exists!
			if (autodeployTarget == null || autodeployTarget.isEmpty()) {
				return; // no op..
			}

			if (!autodeployTarget.endsWith("/"))
				autodeployTarget += "/";
			File targetTest = new File(autodeployTarget);
			if (!targetTest.exists()) {
				log.error("autodeploy directory '" + autodeployTarget
						+ "' dose not exist. Please check your manik properties for this project.");
				return;
			}

			// verify if sourceFileName includes a maven /target folder pattern
			if (sourceFilePath.indexOf("/target/") > -1) {
				// in this case only root artifacts will be copied. No .war
				// files included in a /target sub folder!
				if (sourceFilePath.indexOf('/',
						sourceFilePath.indexOf("/target/") + 8) > -1)
					return; // no op!

			}

			targetFilePath = autodeployTarget + sourceFileName;

			// disable hotdeploy mode!
			hotDeployMode = false;

		} else {
			// Hotdepoyment mode!
			if (hotdeployTarget == null)
				// no hotdeployTarget defined
				return;

			// optimize path....
			if (!hotdeployTarget.endsWith("/"))
				hotdeployTarget += "/";

			// compute the target path....
			targetFilePath = computeTarget();

			// enable hotdeploy mode!
			hotDeployMode = true;
		}

		// if the target file was not computed return....
		if (targetFilePath == null)
			return;

		// check if Autodeploy or Hotdeploy
		if (hotDeployMode) {
			// HOTDEPLOY MODE

			long lStart = System.currentTimeMillis();
			copySingleResource(file, targetFilePath);

			long lTime = System.currentTimeMillis() - lStart;
			// log message..
			if (sourceFileName.endsWith(".ear")
					|| sourceFileName.endsWith(".war"))
				log.info("[AUTODEPLOY]: " + sourceFilePath + " in " + lTime
						+ "ms");
			else
				log.info("[HOTDEPLOY]: " + sourceFilePath + " in " + lTime
						+ "ms");

		} else {
			// AUTODEPLOY MODE

			long lStart = System.currentTimeMillis();
			// check if a .ear or .war file should be autodeplyed in exploded
			// format!...
			if (!explodeArtifact) {
				copySingleResource(file, targetFilePath);
			} else {

				// find extension
				int i = sourceFilePathAbsolute.lastIndexOf(".");
				String sDirPath = sourceFilePathAbsolute.substring(0, i) + "/";
				try {
					File srcFolder = new File(sDirPath);
					File destFolder = new File(targetFilePath);

					copyFolder(srcFolder, destFolder);
				} catch (IOException e) {
					log.info("[AUTODEPLOY]: error - " + e.getMessage());
				}

			}

			// if wildfly support then tough the .deploy file
			if (wildflySupport) {
				try {
					String sDeployFile = targetFilePath + ".dodeploy";
					File deployFile = new File(sDeployFile);
					if (!deployFile.exists())
						new FileOutputStream(deployFile).close();
					deployFile.setLastModified(System.currentTimeMillis());

				} catch (FileNotFoundException e) {
					// console.println("[AUTODEPLOY]: error - " +
					// e.getMessage());
				} catch (IOException e) {
					// console.println("[AUTODEPLOY]: error - " +
					// e.getMessage());
				}
			}

			long lTime = System.currentTimeMillis() - lStart;
			log.info("[AUTODEPLOY]: " + sourceFilePath + " in " + lTime + "ms");
		}

	}

	private void copySingleResource(File file, String targetFilePath) {

		// now copy / delete the file....
		OutputStream out = null;
		InputStream is = null;
		try {
			// Copy the file....
			is = new FileInputStream(file);
			File fOutput = new File(targetFilePath);
			out = new FileOutputStream(fOutput);
			byte buf[] = new byte[1024];
			int len;
			while ((len = is.read(buf)) > 0) {
				out.write(buf, 0, len);
			}

		} catch (IOException ex) {
			// unable to copy file
			log.error(ex.getMessage());
		} finally {
			try {
				if (out != null) {
					out.close();
				}
				if (is != null) {
					is.close();
				}
			} catch (IOException e) {
				log.error("closing stream: " + e.getMessage());
			}

		}

	}

	private static void copyFolder(File src, File dest) throws IOException {

		if (src.isDirectory()) {
			// if directory not exists, create it
			if (!dest.exists()) {
				dest.mkdir();
			}

			// list all the directory contents
			String files[] = src.list();

			for (String file : files) {
				// construct the src and dest file structure
				File srcFile = new File(src, file);
				File destFile = new File(dest, file);
				// recursive copy
				copyFolder(srcFile, destFile);
			}

		} else {
			// if file, then copy it
			// Use bytes stream to support all file types
			InputStream in = new FileInputStream(src);
			OutputStream out = new FileOutputStream(dest);

			byte[] buffer = new byte[1024];

			int length;
			// copy the file content in bytes
			while ((length = in.read(buffer)) > 0) {
				out.write(buffer, 0, length);
			}

			in.close();
			out.close();
		}
	}

	private String computeTarget() {
		File folder = null;

		// hotdeplyoment mode
		// test if deployment is enabled
		if (hotdeployTarget == null)
			return null;

		/* case-2 case a and b included */
		// test if the sourcefile contains a source path which needs to be
		// removed ?
		for (String value : IGNORE_SUBDIRECTORIES) {
			if (sourceFilePath.contains(value)) {

				String path = sourceFilePath.substring(sourceFilePath
						.indexOf(value) + value.length() - 0);

				// now test if the target folder is a web application and the
				// sourcfile is a /classes/ file
				// - test for /WEB-INF/ folder
				if (sourceFilePath.contains("/classes/")) {
					folder = new File(hotdeployTarget + "/WEB-INF/");
					if (folder.exists()) {
						// target is web app - so we need to extend the
						// target....
						path = "/WEB-INF/classes/" + path;
						log.info("Target is a web application changed target path to: "
								+ path);
					}
				}

				if (path.indexOf('/') > -1) {
					folder = new File(hotdeployTarget
							+ path.substring(0, path.lastIndexOf('/')));
					// test target folder - if not exists we did not create the
					// path and return null...
					if (!folder.exists()) {
						return null;
					}
				}
				return hotdeployTarget + path;

			}
		}
		return hotdeployTarget + sourceFilePath;

	}

}
