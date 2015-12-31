# fabric-jira-integrator

This is a tool to integrate Fabric (Crashlytics) and JIRA.

## Background

* Integration of Fabric (the crash-reporting system now owned by Twitter) with JIRA is rudimentary at best.
* At my organization, we use components for JIRA issues. This breaks Fabric-JIRA Integration.

I finally got frustrated on New Year's Eve 2015, and wrote this. Happy New Year to me!

## Architecture

* The tool uses Selenium WebDriver to visit a Fabric URL and fetch all issue details.
* It then uses a JIRA REST API to check whether each Fabric Issue is associated with a JIRA Issue.
* If a Fabric issue is *not associated with a JIRA issue*, the issue is created in JIRA (again using the REST API).

# config.properties

The project relies on a config.properties file in the root directory.

Create one with the following structure:

    fabricurl=https://fabric.io       # this is the fabric URL. Typically doesn't need changing.
    fabricloginendpoint=/login        # this is appended to "fabricurl" to create the login URL.
    fabricusername=your fabric username or email address              
    fabricpassword=your fabric password
    fabricorganization=your fabric organization       
    fabricappname=typically the package name of your app
    fabricosname=the OS of your app
    jiraurl=url to your JIRA
    jirausername=a valid JIRA username
    jirapassword=JIRA password
    jiraproject=project where you want to create JIRA issues
    jiraissuetype=Issue type for fabric issues 
    jiraissuecomponent=Issue component.

The URL for a Fabric issue typically takes this format: 

    https://fabric.io/[FABRIC ORGANIZATION]/[APP OS NAME]/apps/[APP PACKAGE NAME]/issues/[UNIQUE HASH]

From this URL you can get the values for fabricorganization, fabricappname, fabricosname.
