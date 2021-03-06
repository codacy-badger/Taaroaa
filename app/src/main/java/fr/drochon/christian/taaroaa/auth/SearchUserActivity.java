package fr.drochon.christian.taaroaa.auth;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import fr.drochon.christian.taaroaa.R;
import fr.drochon.christian.taaroaa.base.BaseActivity;
import fr.drochon.christian.taaroaa.model.User;

public class SearchUserActivity extends BaseActivity {

    // DESIGN
    private RecyclerView mRecyclerViewUser;
    // FOR DATA
    private AdapterSearchedUser mAdapterSearchedUser;
    private List<User> listUsers;


    // --------------------
    // LIFECYCLE
    // --------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_user);

        mRecyclerViewUser = findViewById(R.id.recyclerViewSearchedUser);
        SearchView searchView = findViewById(R.id.searchbar_user);

        listUsers = new ArrayList<>();

        // Test performance d'affichage de tous les users
        final Trace myTrace = FirebasePerformance.getInstance().newTrace("searchUserActivityShowAllUsers_trace");
        myTrace.start();

        configureRecyclerView();
        myTrace.stop();

        configureToolbar();
        this.giveToolbarAName(R.string.account_search_name);

        // --------------------
        // LISTENERS
        // --------------------

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String name) {
                return true;
            }

            //Configure Adapter & RecyclerView
            @Override
            public boolean onQueryTextChange(String newText) {

                // Test performance de recherche users filtrés
                final Trace myTrace1 = FirebasePerformance.getInstance().newTrace("searchUserActivityShowFilteredlUsers_trace");
                myTrace.start();

                // filtre d'affichage sur la liste des users (grace aux conditions "startat" et "endat" de la requete
                if (!newText.equals(""))
                    mAdapterSearchedUser = new AdapterSearchedUser(generateOptionsForAdapter(getFilteredUser(newText)));
                else
                    mAdapterSearchedUser = new AdapterSearchedUser(generateOptionsForAdapter(getAllUsers()));

                mAdapterSearchedUser.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                    @Override
                    public void onItemRangeInserted(int positionStart, int itemCount) { // c'est cette ligne de code qui insere les données dans la recyclerview
                        mRecyclerViewUser.smoothScrollToPosition(mAdapterSearchedUser.getItemCount()); // Scroll to bottom on new messages
                    }
                });
                mRecyclerViewUser.setLayoutManager(new LinearLayoutManager(SearchUserActivity.this)); // layoutmanager indique comment seront positionnés les elements (linearlayout)
                mRecyclerViewUser.setAdapter(mAdapterSearchedUser);// l'adapter s'occupe du contenu

                myTrace1.stop();
                return true;
            }
        });
    }


    // --------------------
    // TOOLBAR
    // --------------------

    @Override
    public int getFragmentLayout() {
        return 0;
    }

    /**
     * Fait appel au fichier xml menu pour definir les icones.
     * Definit differentes options dans le menu caché.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.covoit_search_vehicle_menu, menu);
        return true; // true affiche le menu
    }


    // --------------------
    // UI
    // --------------------

    /**
     * recuperation  du clic d'un user sur une option de la toolbar.
     * On utilise un switch ici car il peut y avoir plusieurs options.
     * Surtout ne pas oublier le "true" apres chaque case sinon, ce sera toujours le dernier case qui sera executé!
     *
     * @param item item de la toolbar
     * @return option de la toolbar
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return optionsToolbar(this, item);
    }

    /**
     * Configuration de la recyclerview
     * Cette methode créé l'adapter et lui passe en param pas mal d'informations 'comme par ex l'objet FireStoreRecyclerOptions generé par la methode
     * generateOptionsForAdapter.
     */
    private void configureRecyclerView() {

        //Configure Adapter & RecyclerView
        mAdapterSearchedUser = new AdapterSearchedUser(generateOptionsForAdapter(getAllUsers()));
        mAdapterSearchedUser.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) { // c'est cette ligne de code qui insere les données dans la recyclerview
                mRecyclerViewUser.smoothScrollToPosition(mAdapterSearchedUser.getItemCount()); // Scroll to bottom on new messages
            }
        });
        mRecyclerViewUser.setLayoutManager(new LinearLayoutManager(this)); // layoutmanager indique comment seront positionnés les elements (linearlayout)
        mRecyclerViewUser.setAdapter(this.mAdapterSearchedUser);// l'adapter s'occupe du contenu
    }


    // --------------------
    // SEARCH REQUESTS
    // --------------------

    /**
     * La methode generateOptionsForAdapter utilise une requete passée en prama, recupérée depuis un methode definit dans la classe.
     * Cette requete permettra à la recyclerview d'afficher en temps reel le resultat de cette requete (la liste des utilisateurs en bdd, triés ou non).
     */
    private FirestoreRecyclerOptions<User> generateOptionsForAdapter(Query query) {
        return new FirestoreRecyclerOptions.Builder<User>()
                .setQuery(query, User.class)
                .setLifecycleOwner(this)
                .build();
    }

    /**
     * Requete en bdd pour recuperer tous les cours existants
     *
     * @return query
     */
    private Query getAllUsers() {

        // Test performance de recherche users filtrés
        final Trace myTrace2 = FirebasePerformance.getInstance().newTrace("searchUserActivityGetAllUsersQuery_trace");
        myTrace2.start();

        Query mQuery = setupDb().collection("users").orderBy("nom", Query.Direction.ASCENDING);
        mQuery.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                // condition de creation d'un user ou affichage simple d'un message indiquant que l'user existe dejà en bdd.
                // Avec les uid, il ne peut y avoir de doublon, on peut donc etre sur qu'il n'y a qu'un seule doc qui existe s'il en existe un.
                if (documentSnapshots != null && documentSnapshots.size() != 0) {
                    List<DocumentSnapshot> users = documentSnapshots.getDocuments();
                    for (int i = 0; i < users.size(); i++) {
                        Log.e("TAG", "Le document existe !");
                        // liste des docs
                        readDataInList(users);

                        myTrace2.stop();
                    }
                }
            }
        });
        return mQuery;
    }

    /**
     * Methode permettant de filtrer les noms saisis dans la barre de recherche
     *
     * @return query
     */
    private Query getFilteredUser(final String nom) {

        // Test performance de recherche users filtrés
        final Trace myTrace3 = FirebasePerformance.getInstance().newTrace("searchUserActivityGetFilteredUsersQuery_trace");
        myTrace3.start();

        Query mQ = setupDb().collection("users").orderBy("nom").startAt(nom).endAt(nom + '\uf8ff');
        mQ.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (queryDocumentSnapshots != null && queryDocumentSnapshots.size() != 0) {
                    //List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                    filter(listUsers, nom);

                    myTrace3.stop();

                }
            }
        });
        return mQ;
    }

    /**
     * Methode permettant de recuperer l'integralité de la liste des snapshots et d'en faire des objets "User"
     *
     * @param documentSnapshot liste de documents comportant les données
     */
    private void readDataInList(final List<DocumentSnapshot> documentSnapshot) {

        // un DocumentReference fait référence à un emplacement de document dans une base de données Firestore et peut être utilisé pour
        // écrire, lire ou écouter l'emplacement. Il peut exister ou non un document à l'emplacement référencé.
        for (int i = 0; i < documentSnapshot.size(); i++) {
            DocumentSnapshot doc = documentSnapshot.get(i); //Un DocumentSnapshot contient des données lues à partir d'un document dans la base de données Firestore.
            new User(doc.getId(), Objects.requireNonNull(doc.get("nom")).toString(), Objects.requireNonNull(doc.get("prenom"))
                    .toString(), Objects.requireNonNull(doc.get("licence")).toString(), Objects.requireNonNull(doc.get("email")).toString(),
                    Objects.requireNonNull(doc.get("niveau")).toString(), Objects.requireNonNull(doc.get("fonction")).toString());
        }
    }

    /**
     * Methode permettant de filtrer la liste des utilisateurs affichés grace à la barre de recherche
     *
     * @param models  modele de données User
     * @param nomUser nom de l'user
     */
    private void filter(List<User> models, String nomUser) {
        final String lowerCaseQuery = nomUser.toLowerCase();

        List<User> filteredModelList = new ArrayList<>();
        for (User model : models) {
            final String text = model.getNom().toLowerCase();
            if (text.contains(lowerCaseQuery)) {
                filteredModelList.add(model);
            }
        }
    }
}
