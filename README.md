auto-deploy-maven-plugin
========================

A maven plugin for deploy a war/ear project to a specific folder eg. a servers autodeploy folder.

Install
-----
	mvn install

Usage
-----

    <build>
		<plugins>
			<plugin>
				<groupId>de.sisao</groupId>
				<artifactId>auto-deploy-maven-plugin</artifactId>
				<version>1.0-SNAPSHOT</version>
				<configuration>
					<deploypath>C:\\Path\\To\\Your\\Servers\\Autodeploy\\Folder</deploypath>
				</configuration>
				<executions>
					<execution>
						<phase>install</phase>
						<goals>
							<goal>auto-deploy</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
