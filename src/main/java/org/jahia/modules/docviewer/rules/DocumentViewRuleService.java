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

package org.jahia.modules.docviewer.rules;

import javax.jcr.RepositoryException;

import org.drools.spi.KnowledgeHelper;
import org.jahia.modules.docviewer.DocumentViewService;
import org.jahia.services.content.rules.AddedNodeFact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class for converting documents from the right-hand-side (consequences) of rules into SWF files or generating thumbnails.
 * 
 * @author Sergiy Shyrkov
 */
public class DocumentViewRuleService {

    private static Logger logger = LoggerFactory.getLogger(DocumentViewRuleService.class);

    private DocumentViewService viewService;

    /**
     * Converts the specified file node into SWF file.
     * 
     * @param nodeFact
     *            the node to be converted
     * @param overwriteIfExists
     *            is set to true, the existing file should be overwritten if exists; otherwise the new file name will be generated
     *            automatically.
     * @param drools
     *            the rule engine helper class
     * @throws RepositoryException
     *             in case of an error
     */
    public void convert(AddedNodeFact nodeFact, boolean overwriteIfExists, KnowledgeHelper drools)
            throws RepositoryException {
        try {
            viewService.convert(nodeFact.getNode(), overwriteIfExists);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Generates thumbnail of the specified site for the provided document node.
     * 
     * @param nodeFact
     *            the node to create a view for
     * @param thumbnailName
     *            the name of the thumbnail node
     * @param thumbnailSize
     *            the size of the generated thumbnail
     * @param drools
     *            the rule engine helper class
     * @throws RepositoryException
     *             in case of an error
     */
    public void createThumbnail(AddedNodeFact nodeFact, String thumbnailName, int thumbnailSize,
            KnowledgeHelper drools) throws RepositoryException {
        try {
            viewService.createThumbnail(nodeFact.getNode(), thumbnailName, thumbnailSize);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Creates the SWF view for the specified file node.
     * 
     * @param nodeFact
     *            the node to create a view for
     * @param drools
     *            the rule engine helper class
     * @throws RepositoryException
     *             in case of an error
     */
    public void createView(AddedNodeFact nodeFact, KnowledgeHelper drools)
            throws RepositoryException {
        try {
            viewService.createView(nodeFact.getNode());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Injects an instance of the {@link DocumentViewService}.
     * 
     * @param converterService
     *            an instance of the {@link DocumentViewService}
     */
    public void setViewService(DocumentViewService converterService) {
        this.viewService = converterService;
    }
}