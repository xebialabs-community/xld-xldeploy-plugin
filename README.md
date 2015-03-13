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
