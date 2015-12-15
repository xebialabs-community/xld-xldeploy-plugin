# XLD XL Deploy plugin #

This document describes the functionality provided by the XLD XL Deploy plugin.

See the **XL Deploy Reference Manual** for background information on XL Deploy and deployment concepts.

## Overview

The XLD XL Deploy plugin is a XL Deploy plugin that adds capabilities to "deploy" an application to another XL Deploy instance.

###Features

* Deploy an application to another instance of XL Deploy

## Requirements

* **XL Deploy requirements**
	* **XL Deploy**: version 4.0+
	* **Other XL Deploy Plugins**: None
	* Apache HTTP Client mime jar version 4.2.1 (to match the http libraries already available in XL Deploy)

## Installation

Place the plugin JAR file into your `SERVER_HOME/plugins` directory. 

## Build it

Build needs some jar files from XLD installation. 

`XLDEPLOY_HOME=/path/to/xld-installation gradlew clean assemble`

This command generates a jar that can be installed.

## XLD deployments

By adding a xldeploy.DarPackage deployable to a package you are able to deploy that package to a xldeploy.Server container. This uses all the existing mechanisms (including security and updating) of XL Deploy.
If a package version already exists on the target instance it will not fail the deployment but will log that the package was already imported in the deployment log.

## Configuration

The XL Deploy plugin adds synthetic properties to specific CIs in XL Deploy that are used to deploy to another XLD instance:
* *xldeploy.DarPackage*: this deployable enables the deployment of the package to an XLD instance. No properties are needed.
* *xldeploy.Server*: this defines the target XLD container. This can defined on any host (including a localhost) as it contains all information to establish a http(s) connection to the target XLD instance.

## Options

On the xldeploy.Server CI you can specify the following flags:
* Use Https: check this when the target instance of XLD is using XLD.
* Ignore SSL warnings: check this if XLD is running under a self-signed certificate that has not been added to the source instance' TrustStore. More infor about adding certificates to a truststore can be found here: * Ignore SSL warnings: check this if XLD is running under a self-signed certificate that has not been added to the source instance' TrustStore. More infor about adding certificates to a truststore can be found here: https://docs.xebialabs.com/xl-deploy/how-to/configure-the-cli-to-trust-the-xl-deploy-server-with-a-self-signed-certificate.html
* Ensure Same Path: this is to ensure that the application path on the source instance also exists on the target instance. When uploading a package that Application (version) will be added to any existing Application already in the repository. If the Application doesn't already exist the Application will be added to the Applications root. This however means that permissions may not apply to that Application. This setting assumes that the permission structure is the same between source and target. Before importing the package it checks if the source path is available on the target instance. If it's not it fails the deployment.

 
