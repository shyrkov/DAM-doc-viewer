/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2011 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */

package org.jahia.modules.docviewer;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.jcr.Binary;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import net.coobird.thumbnailator.Thumbnails;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.value.BinaryImpl;
import org.jahia.api.Constants;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.transform.DocumentConverterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Document to SWF converter service that uses pdf2swf from SWFTools for file conversion. Additionally a thumbnails can be generated for a
 * document.
 * 
 * @author Sergiy Shyrkov
 */
public class DocumentViewService {

    private static Logger logger = LoggerFactory.getLogger(DocumentViewService.class);

    /**
     * Checks if the specified mime type belongs to one of the specified groups (like pdf,word,openoffice, etc.).
     * 
     * @param mimeType
     *            the mime type to be checked
     * @param mimeTypeGroup
     *            the group (or multiple groups, separated by comma) the specified mime type should belong to
     * @return if the specified mime type belongs to one of the specified groups (like pdf,word,openoffice, etc.)
     */
    public static boolean isMimeTypeGroup(String mimeType, String mimeTypeGroup) {
        return isMimeTypeGroup(mimeType, StringUtils.split(mimeTypeGroup, ", "));
    }

    /**
     * Checks if the specified mime type belongs to one of the specified groups (like pdf,word,openoffice, etc.).
     * 
     * @param mimeType
     *            the mime type to be checked
     * @param mimeTypeGroups
     *            the groups the specified mime type should belong to
     * @return if the specified mime type belongs to one of the specified groups (like pdf,word,openoffice, etc.)
     */
    public static boolean isMimeTypeGroup(String mimeType, String... mimeTypeGroups) {
        if (mimeType == null) {
            return false;
        }

        boolean found = false;
        for (String grp : mimeTypeGroups) {
            List<String> mimeTypes = JCRContentUtils.getInstance().getMimeTypes().get(grp);
            if (mimeTypes == null) {
                continue;
            }
            for (String mime : mimeTypes) {
                if (mime.contains("*")) {
                    found = Pattern.matches(
                            StringUtils.replace(StringUtils.replace(mime, ".", "\\."), "*", ".*"),
                            mimeType);
                } else {
                    found = mime.equals(mimeType);
                }
                if (found) {
                    break;
                }
            }
            if (found) {
                break;
            }
        }

        return found;
    }

    private DocumentConverterService documentConverterService;

    private boolean enabled;

    private PDF2ImageConverter pdf2imageConverterService;

    private PDF2SWFConverterService pdf2swfConverterService;

    private String[] supportedDocumentFormats;

    private String thumbnailImageFormat = "png";

    /**
     * Performs the conversion of the specified file into the SWF, possible using an intermediate conversion to PDF if needed and it is it
     * supported.
     * 
     * @param sourceFile
     *            the original file to be converted
     * @param sourceContentType
     *            the mime type of the original file
     * @return the file with the result of the conversion
     * @throws Exception
     *             in case of a conversion error
     */
    public File convert(File sourceFile, String sourceContentType) throws Exception {
        File pdfFile = convertToPDF(sourceFile, sourceContentType);
        try {
            return pdf2swfConverterService.convert(pdfFile);
        } finally {
            FileUtils.deleteQuietly(pdfFile);
        }
    }

    /**
     * Converts the specified document file node into SWF file and creates the SWF file node in the same "folder".
     * 
     * @param fileNode
     *            the node to be converted
     * @param overwriteIfExists
     *            is set to true, the existing file should be overwritten if exists; otherwise the new file name will be generated
     *            automatically.
     * @throws RepositoryException
     *             in case of an error
     */
    public void convert(JCRNodeWrapper fileNode, boolean overwriteIfExists)
            throws RepositoryException {
        if (!isEnabled() || supportedDocumentFormats == null) {
            logger.info("Conversion service is not enabled." + " Skip converting node {}",
                    fileNode.getPath());
            return;
        }

        long timer = System.currentTimeMillis();

        if (fileNode.isNodeType("nt:file")
                && isMimeTypeGroup(fileNode.getFileContent().getContentType(),
                        supportedDocumentFormats)) {
            File inFile = null;
            File outFile = null;
            try {
                inFile = createTempFile();
                outFile = convert(JCRContentUtils.downloadFileContent(fileNode, inFile), fileNode
                        .getFileContent().getContentType());
                if (outFile != null) {
                    JCRNodeWrapper folder = fileNode.getParent();
                    String newName = StringUtils.substringBeforeLast(fileNode.getName(), ".")
                            + ".swf";
                    if (!overwriteIfExists) {
                        newName = JCRContentUtils.findAvailableNodeName(folder, newName);
                    }

                    BufferedInputStream convertedStream = new BufferedInputStream(
                            new FileInputStream(outFile));
                    JCRNodeWrapper targetNode = null;
                    try {
                        targetNode = folder.uploadFile(newName, convertedStream,
                                "application/x-shockwave-flash");
                        if (targetNode != null) {
                            targetNode.getSession().save();
                        }
                    } finally {
                        IOUtils.closeQuietly(convertedStream);
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug(
                                "Converted node {} into {} in {} ms",
                                new Object[] { fileNode.getPath(),
                                        targetNode != null ? targetNode.getPath() : null,
                                        (System.currentTimeMillis() - timer) });
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                FileUtils.deleteQuietly(inFile);
                FileUtils.deleteQuietly(outFile);
            }
        } else {
            logger.warn("Path should correspond to a file node with one"
                    + " of the supported formats {}. Skipping node {}", supportedDocumentFormats,
                    fileNode.getPath());
        }
    }

    /**
     * Performs the conversion of the specified file into the PDF document.
     * 
     * @param sourceFile
     *            the original file to be converted
     * @param sourceContentType
     *            the mime type of the original file
     * @return the file with the result of the conversion
     * @throws Exception
     *             in case of a conversion error
     */
    public File convertToPDF(File sourceFile, String sourceContentType) throws Exception {
        if (isMimeTypeGroup(sourceContentType, "pdf")) {
            return sourceFile;
        } else if (!documentConverterService.isEnabled()) {
            logger.warn(
                    "Document converter service is not enabled. Cannot convert file {} into a PDF.",
                    sourceFile);
            return null;
        } else {
            return documentConverterService.convert(sourceFile, sourceContentType,
                    "application/pdf");
        }
    }

    protected File createTempFile() throws IOException {
        return File.createTempFile("doc-viewer", null);
    }

    /**
     * Generates thumbnails for the specified document node.
     * 
     * @param fileNode
     *            the node to generate thumbnails for
     * @param thumbnailName
     *            the name of the thumbnail node
     * @param thumbnailSize
     *            the size of the generated thumbnail
     * @throws RepositoryException
     *             in case of an error
     */
    public void createThumbnail(JCRNodeWrapper fileNode, String thumbnailName, int thumbnailSize)
            throws RepositoryException {
        if (!isEnabled() || supportedDocumentFormats == null) {
            logger.info("Conversion service is not enabled." + " Skip converting node {}",
                    fileNode.getPath());
            return;
        }

        long timer = System.currentTimeMillis();

        if (fileNode.isNodeType("nt:file")
                && isMimeTypeGroup(fileNode.getFileContent().getContentType(),
                        supportedDocumentFormats)) {
            BufferedImage image = null;
            BufferedImage thumbnail = null;
            try {
                image = getImageOfFirstPage(fileNode);

                if (image != null) {
                    thumbnail = Thumbnails.of(image).size(thumbnailSize, thumbnailSize)
                            .asBufferedImage();
                    JCRNodeWrapper thumbNode = storeThumbnailNode(fileNode, thumbnail,
                            thumbnailName);
                    if (logger.isDebugEnabled()) {
                        logger.debug(
                                "Generated thumbnail {} for node {} in {} ms",
                                new Object[] { thumbNode.getPath(), fileNode.getPath(),
                                        (System.currentTimeMillis() - timer) });
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                if (image != null) {
                    image.flush();
                }
                if (thumbnail != null) {
                    thumbnail.flush();
                }
            }
        } else {
            logger.warn("Path should correspond to a file node with one"
                    + " of the supported formats {}. Skipping node {}", supportedDocumentFormats,
                    fileNode.getPath());
        }
    }

    protected JCRNodeWrapper storeThumbnailNode(JCRNodeWrapper fileNode, BufferedImage thumbnail,
            String thumbnailName) throws RepositoryException, IOException {
        JCRNodeWrapper node = null;

        fileNode.getSession().checkout(fileNode);

        try {
            node = fileNode.getNode(thumbnailName);
        } catch (PathNotFoundException e) {
            node = fileNode.addNode(thumbnailName, Constants.JAHIANT_RESOURCE);
            node.addMixin(Constants.JAHIAMIX_IMAGE);
        }

        if (node.hasProperty(Constants.JCR_DATA)) {
            node.getProperty(Constants.JCR_DATA).remove();
        }

        Binary b = null;
        ByteArrayOutputStream os = null;
        try {
            os = new ByteArrayOutputStream(16 * 1024);
            ImageIO.write(thumbnail, thumbnailImageFormat, os);
            b = new BinaryImpl(os.toByteArray());
            node.setProperty(Constants.JCR_DATA, b);
        } finally {
            os = null;
            if (b != null) {
                b.dispose();
            }
        }
        node.setProperty("j:width", thumbnail.getWidth());
        node.setProperty("j:height", thumbnail.getHeight());
        node.setProperty(Constants.JCR_MIMETYPE, "png".equals(thumbnailImageFormat) ? "image/png"
                : "image/jpeg");
        Calendar lastModified = Calendar.getInstance();
        node.setProperty(Constants.JCR_LASTMODIFIED, lastModified);
        fileNode.setProperty(Constants.JCR_LASTMODIFIED, lastModified);

        return node;
    }

    /**
     * Creates the SWF view for the specified PDF file node.
     * 
     * @param fileNode
     *            the node to create a view for
     * @throws RepositoryException
     *             in case of an error
     */
    public void createView(JCRNodeWrapper fileNode) throws RepositoryException {
        if (!isEnabled() || supportedDocumentFormats == null) {
            logger.info("Conversion service is not enabled." + " Skip converting node {}",
                    fileNode.getPath());
            return;
        }

        long timer = System.currentTimeMillis();

        if (fileNode.isNodeType("nt:file")
                && isMimeTypeGroup(fileNode.getFileContent().getContentType(),
                        supportedDocumentFormats)) {
            File inFile = null;
            File outFile = null;
            try {
                inFile = createTempFile();
                outFile = convert(JCRContentUtils.downloadFileContent(fileNode, inFile), fileNode
                        .getFileContent().getContentType());
                if (outFile != null) {
                    fileNode.getSession().checkout(fileNode);
                    JCRNodeWrapper swfNode = null;
                    try {
                        swfNode = fileNode.getNode("swfView");
                    } catch (PathNotFoundException e) {
                        if (!fileNode.isNodeType("jmix:swfDocumentView")) {
                            fileNode.addMixin("jmix:swfDocumentView");
                        }
                        swfNode = fileNode.addNode("swfView", "nt:resource");
                    }

                    BufferedInputStream convertedStream = new BufferedInputStream(
                            new FileInputStream(outFile));
                    try {
                        if (swfNode.hasProperty(Constants.JCR_DATA)) {
                            swfNode.getProperty(Constants.JCR_DATA).remove();
                        }
                        swfNode.setProperty(Constants.JCR_DATA, new BinaryImpl(convertedStream));
                        swfNode.setProperty(Constants.JCR_MIMETYPE, "application/x-shockwave-flash");
                        Calendar lastModified = Calendar.getInstance();
                        swfNode.setProperty(Constants.JCR_LASTMODIFIED, lastModified);
                        fileNode.getSession().save();
                    } finally {
                        IOUtils.closeQuietly(convertedStream);
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("Created SWF view for node {} in {} ms", fileNode.getPath(),
                                System.currentTimeMillis() - timer);
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                FileUtils.deleteQuietly(inFile);
                FileUtils.deleteQuietly(outFile);
            }
        } else {
            logger.warn("Path should correspond to a file node with one"
                    + " of the supported formats {}. Skipping node {}", supportedDocumentFormats,
                    fileNode.getPath());
        }
    }

    protected BufferedImage getImageOfFirstPage(JCRNodeWrapper fileNode) throws Exception {
        BufferedImage image = null;

        String sourceContentType = fileNode.getFileContent().getContentType();
        InputStream pdfInputStream = null;
        File pdfFile = null;
        try {
            if (isMimeTypeGroup(sourceContentType, "pdf")) {
                pdfInputStream = fileNode.getFileContent().downloadFile();
            } else {
                if (!documentConverterService.isEnabled()) {
                    logger.warn(
                            "Document converter service is not enabled. Cannot convert node {} into a PDF.",
                            fileNode.getPath());
                    return null;
                } else {
                    File inFile = null;
                    try {
                        inFile = createTempFile();
                        JCRContentUtils.downloadFileContent(fileNode, inFile);
                        pdfFile = documentConverterService.convert(inFile, sourceContentType,
                                "application/pdf");
                        pdfInputStream = new FileInputStream(pdfFile);
                    } finally {
                        FileUtils.deleteQuietly(inFile);
                    }
                }
            }

            if (pdfInputStream != null) {
                image = pdf2imageConverterService.getImageOfPage(pdfInputStream, 0);
            }
        } finally {
            IOUtils.closeQuietly(pdfInputStream);
            FileUtils.deleteQuietly(pdfFile);
        }

        return image;
    }

    /**
     * Returns <code>true</code> if the conversion service is enabled; <code>false</code> otherwise.
     * 
     * @return <code>true</code> if the conversion service is enabled; <code>false</code> otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    public void setDocumentConverterService(DocumentConverterService documentConverterService) {
        this.documentConverterService = documentConverterService;
    }

    /**
     * Enables or disables the conversion service
     * 
     * @param enabled
     *            set to <code>true</code> to enable the service
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setPdf2imageConverterService(PDF2ImageConverter pdf2imageConverterService) {
        this.pdf2imageConverterService = pdf2imageConverterService;
    }

    public void setPdf2swfConverterService(PDF2SWFConverterService pdf2swfConverterService) {
        this.pdf2swfConverterService = pdf2swfConverterService;
    }

    public void setSupportedDocumentFormats(Set<String> supportedDocumentFormats) {
        this.supportedDocumentFormats = supportedDocumentFormats != null
                && !supportedDocumentFormats.isEmpty() ? supportedDocumentFormats
                .toArray(new String[] {}) : null;
    }

    public void setThumbnailImageFormat(String thumbnailImageFormat) {
        this.thumbnailImageFormat = thumbnailImageFormat;
    }

}
