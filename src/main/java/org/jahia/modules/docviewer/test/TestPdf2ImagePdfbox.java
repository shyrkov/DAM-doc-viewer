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
package org.jahia.modules.docviewer.test;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.exec.ExecuteException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

/**
 * TODO comment me
 * 
 * @author Sergiy Shyrkov
 * 
 */
public class TestPdf2ImagePdfbox {

    private static final String fileName = "izpack-article-jm-3_06-95_99";
    private static final int imageType = BufferedImage.TYPE_INT_RGB;
    private static final int resolution = 96;
    private static final String imageFormat = "png";

    /**
     * TODO comment me
     * 
     * @param args
     * @throws IOException
     * @throws ExecuteException
     */
    public static void main(String[] args) throws Exception {
        long timer = System.currentTimeMillis();

        PDDocument pdf = PDDocument.load(new File("w:/exploring/pdf2swf/sandbox/" + fileName
                + ".pdf"));
        PDPage page = (PDPage) pdf.getDocumentCatalog().getAllPages().get(0);
        BufferedImage image = page.convertToImage(imageType, resolution);
        BufferedImage thumbnail = generateThumbnail(image, 150);
        ImageIO.write(thumbnail, imageFormat, new File("w:/exploring/pdf2swf/sandbox/" + fileName
                + "a." + imageFormat));
        BufferedImage thumbnail2 = generateThumbnail(image, 350);
        ImageIO.write(thumbnail2, imageFormat, new File("w:/exploring/pdf2swf/sandbox/" + fileName
                + "a_2." + imageFormat));
        image.flush();
        thumbnail.flush();

        System.out.println("Took " + (System.currentTimeMillis() - timer));
    }

    public static BufferedImage generateThumbnail(BufferedImage img, int size) {
        if (img.getWidth() <= size && img.getHeight() <= size) {
            return img;
        }
        int width = size;
        int height = size * img.getHeight() / img.getWidth();
        if (img.getHeight() > img.getWidth()) {
            width = size * img.getWidth() / img.getHeight();
            height = size;
        }
        BufferedImage resized = new BufferedImage(width, height, imageType);
        Graphics2D graphics2D = resized.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(img, 0, 0, width, height, null);
        graphics2D.dispose();

        return resized;

    }
}
