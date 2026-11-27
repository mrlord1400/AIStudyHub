import Model.DAO.DocumentDAO;
import Model.DTO.Document;
import java.time.LocalDateTime;

public class T12Test {

    static int newDoc(DocumentDAO dao, int owner, String title) {
        Document d = new Document();
        d.setUserId(owner);
        d.setTitle(title);
        d.setFileExtension("pdf");
        d.setCloudStorageUrl("/AIStudyHub/uploads/" + owner + "/" + title + ".pdf");
        d.setFileSizeMb(1.0);
        d.setAiParsingStatus("READY");
        d.setSharingPermission("PRIVATE");
        d.setShareLinkToken(java.util.UUID.randomUUID().toString());
        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        return dao.insertDocument(d);
    }

    public static void main(String[] a) throws Exception {
        Db.init();
        int A = Db.user("A", "STUDENT"), B = Db.user("B", "STUDENT");
        DocumentDAO dao = new DocumentDAO();
        int docB = newDoc(dao, B, "b-secret");
        int docA = newDoc(dao, A, "a-doc");
        Db.check("fixture inserted", docB > 0 && docA > 0);

        Db.check("A cannot update B's doc", !dao.updateDocumentInfoForOwner(docB, A, "pwned", null, "PUBLIC", "x"));
        Db.check("B's doc unchanged after A's update attempt", Db.count("SELECT COUNT(*) FROM documents WHERE document_id=? AND title='b-secret' AND sharing_permission='PRIVATE'", docB) == 1);
        Db.check("A cannot change B's sharing permission", !dao.updateSharingPermissionForOwner(docB, A, "PUBLIC"));
        Db.check("A cannot replace B's file", !dao.replaceDocumentFileForOwner(docB, A, "/evil", 1, "pdf", "t", null, "PUBLIC"));
        Db.check("A cannot delete B's doc", !dao.deleteDocumentForOwner(docB, A));
        Db.check("B's doc row still exists", Db.count("SELECT COUNT(*) FROM documents WHERE document_id=?", docB) == 1);
        Db.check("B's doc url unchanged", Db.count("SELECT COUNT(*) FROM documents WHERE document_id=? AND cloud_storage_url LIKE '%/uploads/%'", docB) == 1);

        Db.check("A can update own doc", dao.updateDocumentInfoForOwner(docA, A, "renamed", null, "FRIENDS_ONLY", "/AIStudyHub/uploads/1/a.pdf"));
        Db.check("A's update applied", Db.count("SELECT COUNT(*) FROM documents WHERE document_id=? AND title='renamed' AND sharing_permission='FRIENDS_ONLY'", docA) == 1);
        Db.check("A can change own permission", dao.updateSharingPermissionForOwner(docA, A, "PUBLIC"));
        Db.check("A can delete own doc", dao.deleteDocumentForOwner(docA, A));
        Db.check("A's doc gone", Db.count("SELECT COUNT(*) FROM documents WHERE document_id=?", docA) == 0);
        Db.check("deleting a missing doc returns false", !dao.deleteDocumentForOwner(docA, A));
        Db.done();
    }
}
