/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.export.pdf.job;

import java.util.Collections;
import java.util.List;

import org.xwiki.job.Request;
import org.xwiki.job.api.AbstractCheckRightsRequest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Represents a request to export multiple XWiki documents as PDF.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
@Unstable
public class PDFExportJobRequest extends AbstractCheckRightsRequest
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    private static final String PROPERTY_DOCUMENTS = "documents";

    private static final String PROPERTY_TEMPLATE = "template";

    private static final String PROPERTY_COVER = "cover";

    private static final String PROPERTY_TOC = "toc";

    private static final String PROPERTY_HEADER = "header";

    private static final String PROPERTY_FOOTER = "footer";

    private static final String PROPERTY_TITLE = "title";

    private static final String PROPERTY_SERVER_SIDE = "serverSide";

    private static final String PROPERTY_FILE_NAME = "fileName";

    /**
     * Default constructor.
     */
    public PDFExportJobRequest()
    {
    }

    /**
     * @param request the request to copy
     * @since 14.7RC1
     * @since 14.4.4
     * @since 13.10.9
     */
    public PDFExportJobRequest(Request request)
    {
        super(request);
    }

    /**
     * @return the list of documents to include in the PDF export; the PDF export job will render each of these
     *         documents to HTML and aggregate the results using the PDF template, printing the result to PDF in the
     *         end; the first document in the list is considered the main document and will be used to generate the
     *         cover page, table of contents, header and footer
     */
    public List<DocumentReference> getDocuments()
    {
        return getProperty(PROPERTY_DOCUMENTS, Collections.emptyList());
    }

    /**
     * Sets the list of documents to export as PDF.
     * 
     * @param documents the list of documents to export as PDF
     */
    public void setDocuments(List<DocumentReference> documents)
    {
        setProperty(PROPERTY_DOCUMENTS, documents);
    }

    /**
     * @return the PDF template reference, i.e. the document that controls what is displayed in the cover page, table of
     *         contents, header and footer, as well as the styles used when generating the PDF; the PDF template should
     *         have an object of type {@code XWiki.PDFExport.TemplateClass}
     */
    public DocumentReference getTemplate()
    {
        return getProperty(PROPERTY_TEMPLATE);
    }

    /**
     * Sets the PDF template.
     * 
     * @param template the PDF template to use
     */
    public void setTemplate(DocumentReference template)
    {
        setProperty(PROPERTY_TEMPLATE, template);
    }

    /**
     * @return whether to generate the cover page or not
     */
    public boolean isWithCover()
    {
        return getProperty(PROPERTY_COVER, true);
    }

    /**
     * Sets whether to generate the cover page or not.
     * 
     * @param withCover {@code true} to generate the cover page, {@code false} otherwise
     */
    public void setWithCover(boolean withCover)
    {
        setProperty(PROPERTY_COVER, withCover);
    }

    /**
     * @return whether to generate the table of contents page or not
     */
    public boolean isWithToc()
    {
        return getProperty(PROPERTY_TOC, true);
    }

    /**
     * Sets whether to generate the table of contents page or not.
     * 
     * @param withToc {@code true} to generate the table of contents page, {@code false} otherwise
     */
    public void setWithToc(boolean withToc)
    {
        setProperty(PROPERTY_TOC, withToc);
    }

    /**
     * @return whether to generate the page header or not
     */
    public boolean isWithHeader()
    {
        return getProperty(PROPERTY_HEADER, true);
    }

    /**
     * Sets whether to generate the page header or not.
     * 
     * @param withHeader {@code true} to generate the page header, {@code false} otherwise
     */
    public void setWithHeader(boolean withHeader)
    {
        setProperty(PROPERTY_HEADER, withHeader);
    }

    /**
     * @return whether to generate the page footer or not
     */
    public boolean isWithFooter()
    {
        return getProperty(PROPERTY_FOOTER, true);
    }

    /**
     * Sets whether to generate the page footer or not.
     * 
     * @param withFooter {@code true} to generate the page footer, {@code false} otherwise
     */
    public void setWithFooter(boolean withFooter)
    {
        setProperty(PROPERTY_FOOTER, withFooter);
    }

    /**
     * @return whether to generate the PDF file server-side, as a temporary resource (using a headless browser), or
     *         client-side using the user's browser
     */
    public boolean isServerSide()
    {
        return getProperty(PROPERTY_SERVER_SIDE, false);
    }

    /**
     * Sets whether to generate the PDF file server-side, as a temporary resource, or client-side.
     * 
     * @param serverSide {@code true} to generate the PDF file server-side, {@code false} to let the users print the PDF
     *            file themselves
     */
    public void setServerSide(boolean serverSide)
    {
        setProperty(PROPERTY_SERVER_SIDE, serverSide);
    }

    /**
     * @return whether to render the document title before the document content, using a level 1 heading; when not set
     *         it defaults to {@code true} if {@link #getDocuments()} has multiple entries, otherwise to {@code false},
     *         when a single document is rendered; note that this has an impact on the table of contents (when we
     *         generate one)
     * @since 14.9RC1
     */
    public boolean isWithTitle()
    {
        return getProperty(PROPERTY_TITLE, getDocuments().size() > 1);
    }

    /**
     * Sets whether to render the document title, before the document content, or not.
     *
     * @param withTitle {@code true} to include the document title, {@code false} to omit the document title,
     *            {@code null} to include the document title only when multiple documents are rendered
     * @since 14.9RC1
     */
    public void setWithTitle(Boolean withTitle)
    {
        setProperty(PROPERTY_TITLE, withTitle);
    }

    /**
     * @return the file name proposed by the user agent when the user saves the generated PDF on their file system
     * @since 14.9
     */
    public String getFileName()
    {
        return getProperty(PROPERTY_FILE_NAME);
    }

    /**
     * Sets the file name proposed by the user agent when the user saves the generated PDF on their file system.
     * 
     * @param fileName the PDF file name
     * @since 14.9
     */
    public void setFileName(String fileName)
    {
        setProperty(PROPERTY_FILE_NAME, fileName);
    }
}
