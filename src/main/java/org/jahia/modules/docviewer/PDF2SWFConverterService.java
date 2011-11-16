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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Document to SWF converter service that uses pdf2swf from SWFTools for file conversion.
 * 
 * @author Sergiy Shyrkov
 */
public class PDF2SWFConverterService {

    private static Logger logger = LoggerFactory.getLogger(PDF2SWFConverterService.class);

    private boolean enabled;

    private String executablePath;

    private String parameters;

    /**
     * Converts the provided PDF input file into an SWF file, using a temporary file as output.
     * 
     * @param inputFile
     *            the source file
     * @return the SFW file with the converted content
     * @throws Exception
     *             in case of a conversion error
     */
    public File convert(File inputFile) throws Exception {
        if (!isEnabled()) {
            logger.info("pdf2swf conversion service is not enabled." + " Skip converting file {}",
                    inputFile);
            return null;
        }

        File out = createTempFile();
        convert(inputFile, out);
        return out;
    }

    /**
     * Converts the provided PDF input file into specified SWF output file.
     * 
     * @param inputFile
     *            the source file
     * @param outputFile
     *            the output file descriptor to store converted content into
     * @throws Exception
     *             in case of a conversion error
     */
    public void convert(File inputFile, File outputFile) throws Exception {
        if (!isEnabled()) {
            logger.info("pdf2swf conversion service is not enabled." + " Skip converting file {}",
                    inputFile);
            return;

        }

        long timer = System.currentTimeMillis();

        CommandLine cmd = getConvertCommandLine(inputFile, outputFile);

        if (logger.isDebugEnabled()) {
            logger.debug("Execuiting conversion command: {}", cmd.toString());
        }

        int exitValue = new DefaultExecutor().execute(cmd);

        if (logger.isDebugEnabled()) {
            logger.debug("Conversion from {} to {} done (exit code: {}) in {} ms", new Object[] {
                    inputFile, outputFile, exitValue, (System.currentTimeMillis() - timer) });
        }
    }

    protected File createTempFile() throws IOException {
        return File.createTempFile("doc-viewer", null);
    }

    protected CommandLine getConvertCommandLine(File inputFile, File outputFile) {
        CommandLine cmd = new CommandLine(getExecutablePath());
        cmd.addArgument("${inFile}");
        cmd.addArgument("-o");
        cmd.addArgument("${outFile}");
        cmd.addArguments(getParameters(), false);

        Map<String, File> params = new HashMap<String, File>(2);
        params.put("inFile", inputFile);
        params.put("outFile", outputFile);

        cmd.setSubstitutionMap(params);

        return cmd;
    }

    protected String getExecutablePath() {
        return executablePath;
    }

    protected String getParameters() {
        return parameters;
    }

    /**
     * Returns <code>true</code> if the conversion service is enabled; <code>false</code> otherwise.
     * 
     * @return <code>true</code> if the conversion service is enabled; <code>false</code> otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables the conversion service
     * 
     * @param enabled
     *            set to <code>true</code> to emable the service
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setExecutablePath(String executablePath) {
        this.executablePath = executablePath;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

}
