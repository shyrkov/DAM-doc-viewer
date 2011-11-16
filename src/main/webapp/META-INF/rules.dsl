[condition][]- it is a viewable document = eval(org.jahia.modules.docviewer.DocumentViewService.isMimeTypeGroup(mimeType, "pdf,word,rtf,excel,powerpoint,openoffice"))
[consequence][]Convert {node} to SWF=documentViewService.convert({node}, true, drools);
[consequence][]Create SWF view for the {node}=documentViewService.createView({node}, drools);
[consequence][]Create a document thumbnail named "{thumbnailName}" of size {size}=documentViewService.createThumbnail(node, "{thumbnailName}", {size}, drools);
