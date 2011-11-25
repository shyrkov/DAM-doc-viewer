DAM - Document Viewer Module for Jahia xCM
==========================================

This is a custom Digital Asset Management module for the Jahia xCM platform
that enables previewing of documents as SWF flash and creating thumbnails for
various document formats.

Licensing
---------
This module is free software; you can redistribute it and/or 
modify it under the terms of the GNU General Public License 
as published by the Free Software Foundation; either version 2 
of the License, or (at your option) any later version

Disclaimer
----------
This module was developed by Sergiy Shyrkov and is distributed in the hope that
it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

The status of this development is a "Prototype" and is not targeted to be deployed
and run on a production instance of Jahia xCM.

Requirements
------------
Module is targeted to be deployed to Jahia xCM version 6.6.0.0 or later.

It also has dam-doc-rules module as a dependency.

For document conversion operations and document preview as an SWF it requires an
OpenOffice or LibreOffice + pdf2swf (SWFTools) to be installed and configured.
The path to OpenOffice/LibreOffice installation needs to be configured in Jahia's
WEB-INF/etc/config/jahia.properties and the document conversion service enabled, like::

  ######################################################################
  ### Document Converter Service #######################################
  ######################################################################
  # Set this to true to enable the document conversion service
  documentConverter.enabled = false
  # The fiesystem path to the OpenOffice
  # Usually for Linux it is: /usr/lib/openoffice
  # for Windows: c:/Program Files (x86)/OpenOffice.org 3
  # and for Mac OS X: /Applications/OpenOffice.org.app/Contents
  documentConverter.officeHome = c:/Program Files (x86)/OpenOffice.org 3

For the pdf2swf utility the configuration is located in this module's applicationcontext-doc-viewer.xml file (packaged into a JAR file)::

     <bean id="PDF2SWFConverterService" class="org.jahia.modules.docviewer.PDF2SWFConverterService">
        <!-- set this to true to enable the service -->
        <property name="enabled" value="true"/>
        <property name="executablePath" value="w:/tools/swftools/pdf2swf.exe"/>


3-rd party libraries and their licenses
---------------------------------------
Document viewer directly depends on and includes the following 3-rd party libraries:

* FlexPaper - the open source Web based document viewer for Adobe Flash
  
  License: GNU GPL v3

  Site: http://code.google.com/p/flexpaper/
  
* ICEpdf - for creating images for PDF document pages.
  
  License: Mozilla Public License 1.1

  Site: http://www.icepdf.org/

* Thumbnailator - thumbnail generation library.
  
  License: MIT License

  Site: http://code.google.com/p/thumbnailator/

* Apache Commons Exec - for executing external processes from Java.
  
  License: The Apache Software License, Version 2.0

  Site: http://commons.apache.org/exec/

Some third-party libraries can include other libraries, which are subject to other license terms and conditions.