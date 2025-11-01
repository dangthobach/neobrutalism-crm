package com.neobrutalism.crm.domain.attachment.model;

/**
 * Attachment types
 */
public enum AttachmentType {
    DOCUMENT,       // PDF, Word, Excel, etc.
    IMAGE,          // PNG, JPG, GIF, etc.
    AVATAR,         // User/Customer avatars
    CONTRACT,       // Legal contracts
    INVOICE,        // Invoices and receipts
    REPORT,         // Reports and analytics
    PRESENTATION,   // PowerPoint, etc.
    SPREADSHEET,    // Excel, CSV, etc.
    EMAIL,          // Email attachments
    OTHER           // Other file types
}
