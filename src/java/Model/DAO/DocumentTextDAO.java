package Model.DAO;

import Utils.DBUtils;
import java.sql.*;

/**
 * DAO for the `document_extracted_text` table. Handles saving and retrieving
 * extracted text content from uploaded documents. Used by the AI chatbot VIEW
 * flow to send actual document content to Gemini.
 */
public class DocumentTextDAO {

    // ─── INSERT ──────────────────────────────────────────────────────────────
    /**
     * Saves the extracted text of a document into the database. Called after a
     * file is successfully uploaded and text is extracted.
     *
     * @param documentId the ID of the document this text belongs to
     * @param extractedText the full text content extracted from the file
     * @return true if saved successfully
     */
    public boolean saveExtractedText(int documentId, String extractedText) {
        String sql = "INSERT INTO document_extracted_text (document_id, extracted_text) VALUES (?, ?)";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, documentId);
            ps.setString(2, extractedText);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[DocumentTextDAO] saveExtractedText failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // ─── SELECT ──────────────────────────────────────────────────────────────
    /**
     * Retrieves the extracted text of a document by its ID. Called in
     * ChatBotController when AI requests VIEW/[document name].
     *
     * @param documentId the ID of the document to retrieve text for
     * @return the extracted text string, or null if not found
     */
    public String getExtractedText(int documentId) {
        String sql = "SELECT extracted_text FROM document_extracted_text WHERE document_id = ?";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, documentId);

            try ( ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("extracted_text");
                }
            }

        } catch (SQLException e) {
            System.err.println("[DocumentTextDAO] getExtractedText failed: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────────
    /**
     * Deletes the extracted text for a document by its ID. Called when a
     * document file is replaced with a new version, so the old extracted text
     * can be removed before saving the new one.
     *
     * @param documentId the ID of the document whose extracted text should be
     * deleted
     * @return true if deleted successfully
     */
    public boolean deleteExtractedText(int documentId) {
        String sql = "DELETE FROM document_extracted_text WHERE document_id = ?";

        try ( Connection conn = DBUtils.getConnection();  PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, documentId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[DocumentTextDAO] deleteExtractedText failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}
