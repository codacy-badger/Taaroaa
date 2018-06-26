package fr.drochon.christian.taaroaa.api;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import fr.drochon.christian.taaroaa.model.User;

/**
 * Classe permettant d'implementer le CRUD au sein de l'application pour les utilisateurs.
 */
public class UserHelper {

    private static final String COLLECTION_NAME = "users";

    // --- COLLECTION REFERENCE ---

    /**
     * Recupere la reference d'une collection en utilisant le singleton de FirebasFirestore et en
     * appellant cette collection "users".
     * @return CollectionReference
     */
    public static CollectionReference getUsersCollection(){
        return FirebaseFirestore.getInstance().collection(COLLECTION_NAME);
    }

    // --- CREATE ---

    /**
     * Methode permettant de creer un plongeur. Par defaut, la fonction d'un plongeur créé est PLONGEUR.
     * Si le plongeur evolue dans ses fonctions au sein du club, c'est un  moniteur ou l'admin qui changera sa fonction, pas lui meme.
     * @param uid
     * @param nom
     * @param email
     * @return Task : realise des appels asynchrone
     */
    public static Task<Void> createUser(String uid, String nom, String email) {
        // creation de l'objet User
        User userToCreate = new User(uid, nom, email);

        return UserHelper.getUsersCollection().document(uid).set(userToCreate);
    }

    // --- GET ---

    public static Task<DocumentSnapshot> getUser(String uid){
        return UserHelper.getUsersCollection().document(uid).get();
    }

    // --- UPDATE ---
    /**
     * Mehode permettant de mettre à jour un utilisateur.
     * Auncun utilisateur ne peut updater son adresse email pour eviter de perdre don addresse en la cgngeant trop souvent.
      */
    public static Task<Void> updateUser(String uid, String nom, String prenom, String licence, String email, String niveauPlongeur, String fonction) {

        // creation de l'objet User
        //User userToCreate = new User(uid, nom, prenom, licence, email, niveauPlongeur, fonction);

        //return UserHelper.getUsersCollection().document(uid).set(userToCreate);
        return UserHelper.getUsersCollection().document(uid).update("uid", uid, "nom", nom, "prenom", prenom, "licence", licence, "niveauPlongeur", niveauPlongeur, "fonction", fonction);
    }

    public static Task<Void> updateUser(String uid, String nom) {
        // creation de l'objet User
        User userToCreate = new User(uid, nom);

        return UserHelper.getUsersCollection().document(uid).set(userToCreate);
        //return UserHelper.getUsersCollection().document(uid).update(uid, username);
    }

    /**
     * Methode permettant à un encadrant de changer le statut d'une personne
     * @param uid
     * @param fonctionAuClub
     * @return
     */
    public static Task<Void> updateUserFonction(String uid, String fonctionAuClub) {
        return UserHelper.getUsersCollection().document(uid).update(uid, fonctionAuClub);
    }

    // --- DELETE ---

    public static Task<Void> deleteUser(String uid) {
        return UserHelper.getUsersCollection().document(uid).delete();
    }
}
