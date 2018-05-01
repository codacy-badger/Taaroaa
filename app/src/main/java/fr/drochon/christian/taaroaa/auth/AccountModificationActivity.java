package fr.drochon.christian.taaroaa.auth;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import fr.drochon.christian.taaroaa.R;
import fr.drochon.christian.taaroaa.api.UserHelper;
import fr.drochon.christian.taaroaa.base.BaseActivity;
import fr.drochon.christian.taaroaa.controller.MainActivity;
import fr.drochon.christian.taaroaa.controller.SummaryActivity;
import fr.drochon.christian.taaroaa.model.User;

import static android.widget.Toast.LENGTH_SHORT;

public class AccountModificationActivity extends BaseActivity {

    public static final int GET_USERNAME = 40;
    // identifiant pour identifier la requete REST
    private static final int SIGN_OUT_TASK = 10;
    private static final int DELETE_USER_TASK = 20;
    private static final int UPDATE_USERNAME = 30;
    LinearLayout mLinearLayoutFonctionAdherent;
    TextInputEditText mPrenom;
    TextInputEditText mNom;
    TextInputEditText mLicence;
    Spinner mNiveauPlongeespinner;
    Spinner mFonctionAuClubspinner;
    TextInputEditText mEmail;
    ProgressBar mProgressBar;
    Button mModificationCompte;
    Button mSuppressionCompte;
    TextView mTitrePage;
    Intent mIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_modification);

        //setTitle("Création de compte");
        mTitrePage = findViewById(R.id.titre_page_compte_txt);
        mLinearLayoutFonctionAdherent = findViewById(R.id.linearLayoutFonctionAdherent);
        mPrenom = findViewById(R.id.prenom_txt);
        mNom = findViewById(R.id.nom_txt);
        mLicence = findViewById(R.id.licence_txt);
        mNiveauPlongeespinner = findViewById(R.id.niveau_spinner);
        mNiveauPlongeespinner.setEnabled(false);
        mFonctionAuClubspinner = findViewById(R.id.fonction_spinner);
        mEmail = findViewById(R.id.email_txt);
        mProgressBar = findViewById(R.id.progress_bar);
        mModificationCompte = findViewById(R.id.modificiation_compte_btn);
        //TODO n'afficher le bouton de suppression qu'aux proprieraires des comptes
        mSuppressionCompte = findViewById(R.id.suppression_compte_btn);

        configureToolbar();
        showAttributes();
        // methode à appeler APRES l'initialisation des variables, sinon les variables auront des references null
        this.updateUIWhenCreating(); // recuperation des informations de l'user actuel

        //TODO verifier que tous les champs soient remplis


        // --------------------
        // LISTENERS
        // --------------------

        //Lorsqu'un utilisateur a rempli correctement le formulaire, il est renvoyé à la page Sommaire
        // 1 - lorsqu'un plongeur est connecté, le menu deroulant des fonctions est desactivé; tous les autres champs sont acesssibles.
        // 2 - lorsqu'un encadrant est connecté, tous les chamos sont en lecture seule, sauf le menu deroulant des fonctions
        mModificationCompte.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUserInFirebase(); // update dans firebase
                //updateData(mNom.getText().toString(), mPrenom.getText().toString());
                startSummaryActivity();
            }
        });


        // Suppression d'un compte utilisateur avec alertdialog avant suppression
        mSuppressionCompte.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder adb = new AlertDialog.Builder(AccountModificationActivity.this);
                adb.setTitle(R.string.alertDialog_account);
                adb.setIcon(android.R.drawable.ic_dialog_alert);
                adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        EditText editText = findViewById(R.id.alertdialog_ok_account);
                        Toast.makeText(AccountModificationActivity.this, editText.getText(), Toast.LENGTH_LONG).show();
                        //deleteUserFromFirebase();
                        deleteUser();
                        signOutUserFromFirebase();
                        startMainActivity();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        EditText editText = findViewById(R.id.alertdialog_delete_account);
                        Toast.makeText(AccountModificationActivity.this, editText.getText(), Toast.LENGTH_LONG).show();
                        //finish();
                        //TODO est ce qu'on change d'ecran ou pas ?
                    }
                });
                adb.show(); // affichage de l'artdialog
            }
        });


        // --------------------
        // SPINNERS & REMPLISSAGE
        // --------------------
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapterNiveau = ArrayAdapter.createFromResource(this,
                R.array.niveaux_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapterNiveau.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mNiveauPlongeespinner.setAdapter(adapterNiveau);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.fonctions_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mFonctionAuClubspinner.setAdapter(adapter);
    }

    @Override
    public int getFragmentLayout() {
        return R.layout.activity_account_modification;
    }

    /**
     * Methode appellée lors d'un reaffichage de l'activité
     */
    @Override
    protected void onResume() {
        super.onResume();
        this.updateUIWhenResuming();
    }

    // --------------------
    // TOOLBAR
    // --------------------

    /**
     * Fait appel au fichier xml menu pour definir les icones.
     * Definit differentes options dans le menu caché.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.account_create_menu, menu);
/*
        // recup de l'item de recherche des adherents
        mItemView = menu.findItem(R.id.app_bar_search_adherents);
        searchAndModifPupils();*/

        return true; // true affiche le menu
    }

/*    *//**
     * Methode permettant de gerer la barre de recherche des adherents pour l'affichage de leurs comptes
     * afin que les encadrants puissent les modifier.
     *//*
    private void searchAndModifPupils() {

        android.widget.SearchView searchView = (android.widget.SearchView) mItemView.getActionView();
        searchView.setQueryHint("Modifiez un compte");
        searchView.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        searchView.setOnQueryTextListener(new android.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String name) {
                // decomposition du nom et du prenom recu dans le param name
                String nom = null, prenom = null;
                String[] parts;
                if (name.contains(" ")) {
                    parts = name.split(" ");
                    try {
                        if (parts[1] != null) nom = parts[1];
                        else nom = "";
                    } catch (ArrayIndexOutOfBoundsException e1) {
                        Log.e("TAG", "ArrayOutOfBoundException " + e1.getMessage());
                    }
                    if (parts[0] != null) prenom = parts[0];
                    else prenom = "";
                } else {
                    nom = name;
                    prenom = "";
                }
                System.out.println("clic");
                showSearchedDatas(name, prenom);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                System.out.println(newText);
                return true;
            }
        });
    }*/

    /**
     * recuperation  du clic d'un user.
     * On utilise un switch ici car il peut y avoir plusieurs options.
     * Surtout ne pas oublier le "true" apres chaque case sinon, ce sera toujours le dernier case qui sera executé!
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return optionsToolbar(this, item);
    }

    /**
     * Methode permettant de changer d'ecran lors d'une connexion valide
     */
    private void startSummaryActivity() {
        Intent intent = new Intent(AccountModificationActivity.this, SummaryActivity.class);
        startActivity(intent);
    }

    /**
     * Methode permettant de revenir à la page d'accueil lorsqu'un utilisateur a supprimer son compte
     */
    private void startMainActivity() {
        Intent intent = new Intent(AccountModificationActivity.this, MainActivity.class);
        startActivity(intent);
    }


    // --------------------
    // RECUPERATION DES INFORMATIONS USER
    // --------------------

    /**
     * Methode permettant d'afficher les informations de l'user sur l'ecran AccountCreateActivity lorsqu'un user vient de creer un compte
     */
    private void updateUIWhenCreating() {

        if (this.getCurrentUser() != null) { // retourne un user FirebaseUser. Permettra de remplir toutes les vues de l'activité
            getAndShowUserDatas();
        }
    }

    /**
     * Methode permettant d'afficher les informations d'un user depuis la bdd firestore lorsque le cycle de vie de l'application est à OnResume()
     */
    private void updateUIWhenResuming() {

        if (this.getCurrentUser() != null) { // retourne un user FirebaseUser. Permettra de remplir toutes les vues de l'activité
            getAndShowUserDatas();
        }
    }


    /**
     * Methode permettant de donner acces à la fonction d'un adherent si l'utilisateur connecté est un encadrant ou un initiateur.
     * Ceci permettra de changer la fonction d'un adherent par un encadrant.
     * Cette methode desactive toutes les autres options pour empecher les erreurs de manipulation.
     */
    private void showAttributes() {

        if (this.getCurrentUser() != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference mQuery = db.collection("users").document(getCurrentUser().getUid());

            mQuery.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                    if (documentSnapshot.exists()) {
                        //TODO ajout d'un titre à la page de modif
                        Object ds = documentSnapshot.get("fonction");
                        Object uid = documentSnapshot.get("uid");
                        // moniteur etant sur un autre compte que le sien dès la premiere connexion ??
                        if (ds.equals("Moniteur") && !uid.equals(getCurrentUser().getUid())) {
                            //TODO ajouter un titre à la page
                            mTitrePage.setText(R.string.modifiez_le_compte_d_un_adherent);
                            //mItemView.setVisible(false);
                            mPrenom.setEnabled(false);
                            mNom.setEnabled(false);
                            mLicence.setEnabled(false);
                            mNiveauPlongeespinner.setEnabled(true);
                            mLinearLayoutFonctionAdherent.setVisibility(View.VISIBLE);
                            mProgressBar.setVisibility(View.INVISIBLE);
                            //Affichage du bouton de suppression uniquement aux proprietaires d'un compte
                            mSuppressionCompte.setVisibility(View.INVISIBLE);

                            // moniteur etant sur son propre compte
                        } else if (ds.equals("Moniteur") && uid.equals(getCurrentUser().getUid())) {
                            mTitrePage.setText(R.string.bienvenue_sur_votre_compte);
                            //mItemView.setVisible(true);
                            mPrenom.setEnabled(true);
                            mNom.setEnabled(true);
                            mLicence.setEnabled(true);
                            mNiveauPlongeespinner.setEnabled(true);
                            mLinearLayoutFonctionAdherent.setVisibility(View.VISIBLE);
                            mModificationCompte.setText(R.string.modifiez_votre_compte);
                            //Affichage du bouton de suppression uniquement aux proprietaires d'un compte
                           mSuppressionCompte.setVisibility(View.VISIBLE);

                            // adherent non moniteur sur son propre compte
                        } else if (!ds.equals("Moniteur") && uid.equals(getCurrentUser().getUid())) {
                            mTitrePage.setText(R.string.bienvenue_sur_votre_compte);
                            //mItemView.setVisible(true);
                            mPrenom.setEnabled(true);
                            mNom.setEnabled(true);
                            mLicence.setEnabled(true);
                            mNiveauPlongeespinner.setEnabled(false);
                            mLinearLayoutFonctionAdherent.setVisibility(View.INVISIBLE);
                            mModificationCompte.setText(R.string.modifiez_votre_compte);
                            //Affichage du bouton de suppression uniquement aux proprietaires d'un compte
                            mSuppressionCompte.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });
        }
    }

    // --------------------
    // REST REQUETES
    // --------------------

    /**
     * Cette methode ne comprend pas l'update d'une fonction dans le club, car seul les encadrants du club peuvent
     * le faire, et cette fonctionnalité est donc reservée à une fonction particuliere.
     */
    private void updateUserInFirebase() {

        this.mProgressBar.setVisibility(View.VISIBLE);
        String nom = this.mNom.getText().toString();
        String prenom = this.mPrenom.getText().toString();
        String licence = this.mLicence.getText().toString();
        String niveau = this.mNiveauPlongeespinner.getSelectedItem().toString();
        String fonction = this.mFonctionAuClubspinner.getSelectedItem().toString();
        String email = this.mEmail.getText().toString();

        if (this.getCurrentUser() != null) {
            //TODO alert dialog lorsque tous les champs ne sont pas remplis
            if (!nom.isEmpty() && !nom.equals(getString(R.string.info_no_username_found)) && !prenom.isEmpty() && !email.isEmpty()) { // verification que tous les champs vides soient remplis
                UserHelper.updateUser(this.getCurrentUser().getUid(), nom, prenom, licence, email, niveau, fonction).
                        addOnFailureListener(this.onFailureListener()).
                        addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(AccountModificationActivity.this, R.string.update_account,
                                        Toast.LENGTH_SHORT).show();
                                updateUIAfterRESTRequestsCompleted(UPDATE_USERNAME);
                            }
                        });
            }
        }
    }

    /**
     * Methode permettant de recuperer et d'afficher les données de l'utilisateur actuellement connecté depuis
     * l'activité Sommaire qui a determiné que l'utilisateur voulant afficher ses informations etait enregistré en bdd.
     */
    private void getAndShowUserDatas() {
        mIntent  = getIntent();
        User user = (User) Objects.requireNonNull(mIntent.getExtras()).getSerializable("user");
        assert user != null;
        mNom.setText(user.getNom());
        mPrenom.setText(user.getPrenom());
        mLicence.setText(user.getLicence());
        mNiveauPlongeespinner.setSelection(getIndexSpinner(mNiveauPlongeespinner, user.getNiveauPlongeur()));
        mFonctionAuClubspinner.setSelection(getIndexSpinner(mFonctionAuClubspinner, user.getFonction()));
        mEmail.setText(user.getEmail());
/*        DocumentReference docRef1 = FirebaseFirestore.getInstance().collection("users").document(getCurrentUser().getUid()); // recup ref de l'obj courant en bdd de stockage
        // un DocumentReference fait référence à un emplacement de document dans une base de données Firestore et peut être utilisé pour
        // écrire, lire ou écouter l'emplacement. Il peut exister ou non un document à l'emplacement référencé.

        docRef1.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult(); //Un DocumentSnapshot contient des données lues à partir d'un document dans votre base de données Firestore.
                    if (doc.exists()) {
                        String nom = (String) doc.get("nom");
                        String prenom = (String) doc.get("prenom");
                        String email = (String) doc.get("email");
                        String fonction = (String) doc.get("fonction");
                        String licence = (String) doc.get("licence");
                        String niveau = (String) doc.get("niveau");

                        mNom.setText(nom);
                        mPrenom.setText(prenom);
                        mEmail.setText(email);
                        mLicence.setText(licence);
                        mNiveauPlongeespinner.setSelection(getIndexSpinner(mNiveauPlongeespinner, niveau));
                        mFonctionAuClubspinner.setSelection(getIndexSpinner(mFonctionAuClubspinner, fonction));
                    }
                }
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                    }
                });*/
    }

    /**
     * Methode permettant de recuperer et d'afficher les données de l'utilisateur recherché par un encadrant
     * pour que celui ci effectue une modification du compte de l'adherent.
     */
    private void showSearchedDatas(final String name, final String prenom) {
        CollectionReference docRef1 = FirebaseFirestore.getInstance().collection("users");

        docRef1.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    List<DocumentSnapshot> docs = task.getResult().getDocuments(); //Un DocumentSnapshot contient des données lues à partir d'un document de la base de données Firestore.
                    if (docs.size() != 0) {
                        for (DocumentSnapshot ds : docs) {
                            Map<String, Object> doc = ds.getData();
                            if (doc.get("nom").equals(name) &&
                                    (doc.get("prenom").equals(prenom) || prenom.equals(""))) {
                                // modification du compte d'un adherent que l'on recupere via son champ sur l'ecran
                                String nom = (String) doc.get("nom");
                                String prenom = (String) doc.get("prenom");
                                String email = (String) doc.get("email");
                                String fonction = (String) doc.get("fonction");
                                String licence = (String) doc.get("licence");
                                String niveau = (String) doc.get("niveau");
                                mTitrePage.setText(R.string.modifiez_le_compte_d_un_adherent);
                                mNom.setText(nom);
                                mNom.setEnabled(false);
                                mPrenom.setText(prenom);
                                mPrenom.setEnabled(false);
                                mEmail.setText(email);
                                mLicence.setText(licence);
                                mLicence.setEnabled(false);
                                mNiveauPlongeespinner.setSelection(getIndexSpinner(mNiveauPlongeespinner, niveau));
                                mFonctionAuClubspinner.setSelection(getIndexSpinner(mFonctionAuClubspinner, fonction));
                                //mSuppressionCompte.setVisibility(View.INVISIBLE);
                                break;
                            }
                        }
                    }
                }
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.out.println("nok");
                    }
                });
    }

    /**
     * Methode permettant de mettre à jour les données de n'importe quel adherent du club, mais aussi de mettre à jour
     * les données niveau et fonction d'un adherent par un encadrant. Cette fonction permet aussi de pouvoir updater
     * un utilisateur connecté. Si cet utilisateur est un moniteur, la fonction prend en charge si celui ci veut
     * updater son propre compte ou celui d'un autre adherent.
     */
    private void updateData(final String nom, final String prenom) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();

        this.mProgressBar.setVisibility(View.VISIBLE);
        /*final String nom = this.mNom.getText().toString();
        final String prenom = this.mPrenom.getText().toString();*/
        final String licence = this.mLicence.getText().toString();
        final String niveau = this.mNiveauPlongeespinner.getSelectedItem().toString();
        final String fonction = this.mFonctionAuClubspinner.getSelectedItem().toString();
        final String email = this.mEmail.getText().toString();

        final CollectionReference cr = db.collection("users");
        cr.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    //mise à jour du compte de la personne connectée exceptée moniteur
                    List<DocumentSnapshot> users = task.getResult().getDocuments();
                    for (int i = 0; i < users.size(); i++) {
                        // recuperation des infos de la personne connectée
                        Map<String, Object> user = users.get(i).getData();
                        if (user.get("nom").equals(nom)) { //TODO a changer : condition sur l'uid : recuperer l'uid de l'adherent
                            user.put("nom", nom);
                            user.put("prenom", prenom);
                            user.put("licence", licence);
                            user.put("niveau", niveau);
                            user.put("fonction", fonction);
                            user.put("email", email);

                            if (!nom.isEmpty() && !nom.equals(getString(R.string.info_no_username_found)) && !prenom.isEmpty() && !email.isEmpty()) {
                                db.collection("users").document(getCurrentUser().getUid()).set(user)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Toast.makeText(AccountModificationActivity.this, R.string.update_account,
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(AccountModificationActivity.this, "ERROR" + e.toString(),
                                                        Toast.LENGTH_LONG).show();
                                                Log.d("TAG", e.toString());
                                            }
                                        });
                            } else {
                                //TODO alert dialog lorsque tous les champs ne sont pas remplis
                            }
                            // mise à jour d'une personne autre qu'un encadrant
                        } else if (user.get("fonction").equals("Moniteur") && !user.get("uid").equals(getCurrentUser().getUid())) {

                        /*final String niveau = mNiveauPlongeespinner.getSelectedItem().toString();
                        final String fonction = mFonctionAuClubspinner.getSelectedItem().toString();*/

                            final FirebaseFirestore db = FirebaseFirestore.getInstance();
                            final CollectionReference crf = db.collection("users");
                            crf.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful()) {
                                        List<DocumentSnapshot> u = task.getResult().getDocuments();
                                        for (int i = 0; i < u.size(); i++) {
                                            if (u.get(i).get("nom").equals(nom) &&
                                                    (u.get(i).get("prenom").equals(prenom) || prenom.contentEquals(""))) {
                                                User user1 = new User(u.get(i).getId());

                                                user1.setNom(nom);
                                                user1.setPrenom(prenom);
                                                user1.setLicence(licence);
                                                user1.setNiveauPlongeur(niveau);
                                                user1.setFonction(fonction);
                                                user1.setEmail(email);

                                                // verification que tous les champs vides soient remplis
                                                if (!nom.isEmpty() && !nom.equals(getString(R.string.info_no_username_found)) && !prenom.isEmpty() && !email.isEmpty()) {
                                                    UserHelper.updateUser(user1.getUid(), user1.getNom(), user1.getPrenom(), user1.getLicence(), user1.getEmail(), user1.getNiveauPlongeur(), user1.getFonction());
                                                } else {
                                                    //TODO alert dialog lorsque tous les champs ne sont pas remplis
                                                }
                                            }
                                        }
                                    }
                                }
                            }).addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot documentSnapshots) {
                                    Toast.makeText(AccountModificationActivity.this, R.string.update_account,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                }
            }
        });

    }

    /**
     * Methode permettant de supprimer un utilisateur
     */
    private void deleteUser() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(getCurrentUser().getUid())
                .delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(AccountModificationActivity.this, R.string.alertDialog_delete,
                        LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Methode permettant à un encadrant de supprimer un compte. Retourne un objet de type Task permettant de realiser ces appels de maniere asynchrone
     */
    private void deleteUserFromFirebase() {
        if (this.getCurrentUser() != null) {

            //On supprime un utilisateur de la bdd firestore
            UserHelper.deleteUser(this.getCurrentUser().getUid()).addOnFailureListener(this.onFailureListener());
            //TODO mettre une notification si elle n'arrive pas avoir ajouté le deleteuser ci dessus
            AuthUI.getInstance()
                    .delete(this) // methode utilisée par le singleton authUI.getInstance()
                    .addOnSuccessListener(this, this.updateUIAfterRESTRequestsCompleted(DELETE_USER_TASK));
        }
    }
    /*
     *//**
     * Methode permettant à un utilisateur de se deconnecter retournant un objet de type Task permettant d erealiser ces appels de maniere asynchrone
     *//*
    private void signOutUserFromFirebase() {
        AuthUI.getInstance()
                .signOut(this) // methode utilisée par le singleton authUI.getInstance()
                .addOnSuccessListener(this, this.updateUIAfterRESTRequestsCompleted(SIGN_OUT_TASK));
    }*/
}
