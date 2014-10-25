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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "auto-deploy")
public class DeployerMojo extends AbstractMojo {

	/**
	 * The deploy path
	 */
	@Parameter(property = "deploypath", defaultValue = ".")
	private String deploypath;

	@Component
	private MavenProject project;

	private Log log = getLog();

	public void execute() throws MojoExecutionException {

		log.info("Running auto deploy maven plugin.");

		Deployer deployer = new Deployer();
		File f = new File(project.getBuild().getDirectory() + "\\"
				+ project.getBuild().getFinalName() + "."
				+ project.getPackaging());
		deployer.deployResource(f, deploypath, log);

	}
}
