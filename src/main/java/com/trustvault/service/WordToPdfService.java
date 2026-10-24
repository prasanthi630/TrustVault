
package com.trustvault.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.docx4j.Docx4J;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

import org.springframework.stereotype.Service;

@Service
public class WordToPdfService {

    // Convert DOCX file into PDF
    public byte[] convertDocxToPdf(byte[] docxData)
            throws Exception {

        if (docxData == null || docxData.length == 0) {
            throw new IOException("Word document is empty.");
        }

        // Read Word document
        WordprocessingMLPackage wordPackage =
                WordprocessingMLPackage.load(
                        new ByteArrayInputStream(docxData)
                );

        // Convert Word document to PDF
        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        Docx4J.toPDF(wordPackage, outputStream);

        // Return PDF data
        return outputStream.toByteArray();
    }
}
