package fr.drochon.christian.taaroaa.course;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import fr.drochon.christian.taaroaa.R;
import fr.drochon.christian.taaroaa.base.BaseActivity;
import fr.drochon.christian.taaroaa.model.Course;
import fr.drochon.christian.taaroaa.model.User;

/**
 * creer l'ihm
 * Cette classe permet l'affichage IHM de l'ensemble des messages dans le chat. Contient une recycler view
 * On y implemente MentorChatActivity pour gerer la recyclerview
 * <p>
 * On implement aussi un Listener qui nous permet d'etre alerter si la liste de message est vide grace au callback onDataChanged()
 * pour afficher un message à l'user
 */
public class CoursesPupilsActivity extends BaseActivity implements AdapterCoursesPupils.Listener {

    // CONTIENT LA RECYCLERVIEW


    // FOR DESIGN
    CoordinatorLayout mCoordinatorLayout;
    LinearLayout mLinearLayout;
    CalendarView mCalendarView;
    RecyclerView recyclerView;
    TextView mTextView;
    ScrollView mScrollView;
    FloatingActionButton mFloatingActionButton;
    // FOR DATA
    private AdapterCoursesPupils mAdapterCoursesPupils;
    List<DocumentSnapshot> listSnapshot;
    Date calendrierClique;
    Date calendrierFinJournee;
    static User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_courses_pupils);

        mCoordinatorLayout = findViewById(R.id.pupils_layout_root);
        mLinearLayout = findViewById(R.id.linearLayoutRoot);
        mCalendarView = findViewById(R.id.calendrier_eleves);
        recyclerView = findViewById(R.id.recyclerViewCoursesPupils); // liste des cours
        mTextView = findViewById(R.id.empty_list_textview);
        mScrollView = findViewById(R.id.scrollviewRecyclerView);
        mFloatingActionButton = findViewById(R.id.fab);

        calendrierClique = new Date();
        calendrierFinJournee = new Date();
        listSnapshot = new ArrayList<>();
        user = new User();


        getLevelConnectedUser();
        configureToolbar();

        // --------------------
        // LISTENERS
        // --------------------

        // bouton d'ajout de cours pour les encadrants : renvoi vers la page de gestion des cours si on clique sur l'icone
        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               /* Snackbar.make(view, "Redirection vers la page de gestion des cours", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
                Intent intent = new Intent(CoursesPupilsActivity.this, CoursesManagementActivity.class);
                startActivity(intent);
            }
        });

        // recuperation de la date cliquée
        mCalendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, dayOfMonth);
                //calendrierFinJournee = calendar.getTime();

                // formattage de la date pour le debut et la fin de journée
                DateFormat dateFormatEntree = new SimpleDateFormat("dd MM yyyy", Locale.FRANCE);
                DateFormat dateFormatSortie = new SimpleDateFormat("dd MM yyyy HH:mm:ss", Locale.FRANCE);
                String s = dateFormatEntree.format(calendar.getTime());
                String ss = s.concat(" 00:00:00");
                String sss = s.concat(" 23:59:59");
                try {
                    calendrierClique = dateFormatSortie.parse(ss);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                try {
                    calendrierFinJournee = dateFormatSortie.parse(sss);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                configureRecyclerViewSorted();
            }
        });
    }

    @Override
    public int getFragmentLayout() {
        return R.layout.activity_courses_pupils;
    }


    // --------------------
    // TOOLBAR
    // --------------------

    /**
     * Fait appel au fichier xml menu pour definir les icones du menu toolbar.
     * Defini differentes options dans le menu caché.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.course_pupils_menu, menu);
        return true;
    }

    /**
     * recuperation  du clic d'un user sur une option de la toolbar.
     * On utilise un switch ici car il peut y avoir plusieurs options.
     * Surtout ne pas oublier le "true" apres chaque case sinon, ce sera toujours le dernier case qui sera executé!
     *
     * @param item
     * @return bool
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return optionsToolbar(this, item);
    }


    /**
     * Methode permettant de donner un nom à la page courante de l'application
     * @param title
     */
    private void giveToolbarAName(String title){
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayShowCustomEnabled(true);
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL);

        TextView tv = new TextView(this);
        tv.setGravity(View.TEXT_ALIGNMENT_CENTER);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(20f);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        tv.setText(title);

        ab.setCustomView(tv, layoutParams);
    }

    // --------------------
    // CALLBACK
    // --------------------

    /**
     * Permet d'afficher un message à l'user s'il n'y a pas de messages
     */
    @Override
    public void onDataChanged() {
        mTextView.setVisibility(this.mAdapterCoursesPupils.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    // --------------------
    // ADAPTER ET RECYCLERVIEW
    // --------------------

    /**
     * Configuration de l'adapter et de la recyclerview
     * Cette methode créé l'adapter et lui passe en param l'ensemble des cours existants
     */
    private void configureRecyclerView() {
        mAdapterCoursesPupils = new AdapterCoursesPupils(generateOptionsForAdapter(queryAllCourses()), this);
        mAdapterCoursesPupils.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) { // c'est cette ligne de code qui insere les données dans la recyclerview
                recyclerView.smoothScrollToPosition(mAdapterCoursesPupils.getItemCount()); // Scroll to bottom on new messages
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this)); // layoutmanager indique comment seront positionnés les elements (linearlayout)
        recyclerView.setAdapter(this.mAdapterCoursesPupils);// l'adapter s'occupe du contenu
    }

    /**
     * Configuration de l'adapter et de la recyclerview
     * Cette methode créé l'adapter et lui passe en param la requete de tri des cours en fonction
     * de la date cliquée sur le calendrier
     */
    private void configureRecyclerViewSorted() {
        mAdapterCoursesPupils = new AdapterCoursesPupils(generateOptionsForAdapter(queryCoursesFiltered()), this);
        mAdapterCoursesPupils.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) { // c'est cette ligne de code qui insere les données dans la recyclerview
                recyclerView.smoothScrollToPosition(mAdapterCoursesPupils.getItemCount()); // Scroll to bottom on new messages
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this)); // layoutmanager indique comment seront positionnés les elements (linearlayout)
        recyclerView.setAdapter(this.mAdapterCoursesPupils);// l'adapter s'occupe du contenu
        onDataChanged(); // appel explicite du callback pour l'affichage d'un message en cas d'absence de cours à la date cliquée
    }

    /**
     * La methode generateOptionsForAdapter utilise la methode query, precedemment definit dans la classe MessageHelper
     * permettra à la recyclerview d'afficher en temps reel le resultat de cette requete (la liste de tous les cours (ou triés par date)).
     */
    private FirestoreRecyclerOptions<Course> generateOptionsForAdapter(Query query) {
        return new FirestoreRecyclerOptions.Builder<Course>()
                .setQuery(query, Course.class)
                .setLifecycleOwner(this)
                .build();
    }

    // --------------------
    // UI
    // --------------------

    /**
     * Methode permettant d'afficher le floating button à l'ecran si l'utilisateur est un encadrant ou un initiateur.
     */
    private void showFloatingButton() {

        if (this.getCurrentUser() != null) {
            if(user.getFonction().equals("Moniteur") || user.getFonction().equals("Initiateur")){
                mFloatingActionButton.setVisibility(View.VISIBLE);
            }
        }
    }

    public void notifCompleteAccount() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        // ajouter une couleur à l'icon de warning
        Drawable warning = getResources().getDrawable(android.R.drawable.ic_dialog_alert);
        ColorFilter filter = new LightingColorFilter(Color.RED, Color.BLUE);
        warning.setColorFilter(filter);
        adb.setIcon(warning);
        adb.setTitle("Merci de completer votre compte pour acceder à la liste des cours !");
        adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // rien à appeler. pas la peine de faire de toast
            }
        });
        adb.show();
    }


    // --------------------
    // REQUETES
    // --------------------

    /**
     * Requete en bdd pour recuperer tous les cours existants filtré par le niveau du plongeur
     * connecté sur l'application.
     *
     * @return query
     */
    private Query queryAllCourses() {
        Query mQuery = setupDb().collection("courses").whereEqualTo("niveauDuCours", user.getNiveauPlongeur()).orderBy("horaireDuCours");
        mQuery.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                if (documentSnapshots != null) {
                    if (documentSnapshots.size() != 0) {
                        readDataInList(documentSnapshots.getDocuments());
                    }
                }
            }
        });
        return mQuery;
    }

    /**
     * Methode permettant de requeter avec les conditions suivantes :
     * n'affiche que les cours de la personne connectée + n'affiche que les cours du jour de la date cliquée
     * @return query
     */
    private Query queryCoursesFiltered() {
        Query mQ = setupDb().collection("courses").whereEqualTo("niveauDuCours", user.getNiveauPlongeur()).orderBy("horaireDuCours").startAt(calendrierClique).endAt(calendrierFinJournee);
        mQ.addSnapshotListener(this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                if (documentSnapshots != null) {
                    if (documentSnapshots.size() != 0) {
                        readDataInList(documentSnapshots.getDocuments());
                    }
                }
            }
        });
        return mQ;
    }

    /**
     * Methode permettant de recuperer l'integralité de la liste des snapshots et d'en faire des objets "Course"
     *
     * @param documentSnapshot
     */
    private void readDataInList(final List<DocumentSnapshot> documentSnapshot) {

        // un DocumentReference fait référence à un emplacement de document dans une base de données Firestore et peut être utilisé pour
        // écrire, lire ou écouter l'emplacement. Il peut exister ou non un document à l'emplacement référencé.
        for (int i = 0; i < documentSnapshot.size(); i++) {
            DocumentSnapshot doc = documentSnapshot.get(i); //Un DocumentSnapshot contient des données lues à partir d'un document dans votre base de données Firestore.
            String uid = doc.getId();
            String niveauDuCours = (String) doc.get("niveauDuCours");
            String nomDuMoniteur = (String) doc.get("nomDuMoniteur");
            String sujetDuCours = (String) doc.get("sujetDuCours");
            String typeCours = (String) doc.get("typeCours");
            Date horaireDucours = (Date) doc.get("horaireDuCours");

            Course course = new Course(uid);
            course.setUid(uid);
            course.setNiveauDuCours(niveauDuCours);
            course.setNomDuMoniteur(nomDuMoniteur);
            course.setSujetDuCours(sujetDuCours);
            course.setTypeCours(typeCours);
            course.setHoraireDuCours(horaireDucours);
        }
    }

    /**
     * Methode permettant de recuperer le niveau de l'utilisateur actuellement connecté.
     * Ceci permet d'afficher en titre de page le niveau des cours données, de lancer une requete prenant
     * en parametre le niveau d'un utilisateur dans l'adapter, et d'afficher ou non le floatingbutton
     * en fonction du niveau de l'utilisateur connecté.
     */
    private void getLevelConnectedUser(){
        setupDb().collection("users").whereEqualTo("uid", Objects.requireNonNull(getCurrentUser()).getUid()).get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot documentSnapshots) {
                if(documentSnapshots.size() != 0){
                    List<DocumentSnapshot> ds = documentSnapshots.getDocuments();
                    for( int i = 0; i < ds.size(); i++){
                        Map<String, Object> map = ds.get(i).getData();
                        user.setFonction(map.get("fonction").toString()); // utile pour l'affichage du floatingbutton
                        // affichage de la toolbar avec le niveau de la personne connectée
                        user.setNiveauPlongeur(map.get("niveau").toString());
                        String s = "Cours de niveau " + user.getNiveauPlongeur();
                        giveToolbarAName(s);
                        configureRecyclerView();
                        showFloatingButton();
                    }
                }
            }
        });
    }
}
