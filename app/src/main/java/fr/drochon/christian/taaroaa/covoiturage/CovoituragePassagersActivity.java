package fr.drochon.christian.taaroaa.covoiturage;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import fr.drochon.christian.taaroaa.R;
import fr.drochon.christian.taaroaa.api.CovoiturageHelper;
import fr.drochon.christian.taaroaa.base.BaseActivity;
import fr.drochon.christian.taaroaa.model.Covoiturage;
import fr.drochon.christian.taaroaa.notifications.TimeAlarmCovoiturageAller;
import fr.drochon.christian.taaroaa.notifications.TimeAlarmCovoiturageRetour;

public class CovoituragePassagersActivity extends BaseActivity {


    TextInputEditText mNomConducteur;
    TextInputEditText mDateDepart;
    TextInputEditText mDateretour;
    TextInputEditText mNbPlaceDispo;
    TextInputEditText mTypeVehicule;
    LinearLayout mTitreInscription;
    LinearLayout mLinearChampsDynamiques;
    TextView mTitrePassager;
    TextInputEditText mNbPassagerInput;
    Button mReservation;
    Intent mIntent;
    ProgressBar mProgressBar;
    TextInputEditText mFieldNamePassengers;
    // DATAS
    int inputs;
    List<TextInputEditText> listNamePassengers;
    private static Covoiturage covoiturage;
    AlarmManager mAlarmManagerAller;
    AlarmManager mAlarmManagerRetour;

    // --------------------
    // LIFECYCLE
    // --------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_covoiturage_passagers);

        mNomConducteur = findViewById(R.id.nom_conducteur_txt);
        mDateDepart = findViewById(R.id.date_depart_txt);
        mDateretour = findViewById(R.id.date_retour_txt);
        mNbPlaceDispo = findViewById(R.id.nb_place_dispo_txt);
        mTypeVehicule = findViewById(R.id.type_vehicule_txt);
        mReservation = findViewById(R.id.reservation_covoit_btn);
        mProgressBar = findViewById(R.id.progress_bar);
        mTitreInscription = findViewById(R.id.titre_inscription);
        mLinearChampsDynamiques = findViewById(R.id.linearLayoutDynamique);
        mTitrePassager = findViewById(R.id.nom_passager_txt);
        mNbPassagerInput = findViewById(R.id.nb_passager_input);

        listNamePassengers = new ArrayList<>();
        //  les AlarmManager permettront de réveiller le téléphone et d'executer du code à une date précise
        mAlarmManagerAller = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mAlarmManagerRetour = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        this.configureToolbar();
        this.giveToolbarAName(R.string.covoit_passager_name);
        this.updateUIWhenCreating();

        // --------------------
        // LISTENERS
        // --------------------

        mNbPassagerInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                showFieldsNamePassengers(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mReservation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createPassagerInCovoiturage();
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        this.updateUIWhenResuming();
    }

    @Override
    public int getFragmentLayout() {
        return 0;
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
        getMenuInflater().inflate(R.menu.course_pupils_menu, menu);
        return true; // true affiche le menu
    }

    /**
     * recuperation  du clic d'un user.
     * On utilise un switch ici car il peut y avoir plusieurs options.
     * Surtout ne pas oublier le "true" apres chaque case sinon, ce sera toujours le dernier case qui sera executé!
     *
     * @param item menuitem
     * @return optionsToolBar
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return optionsToolbar(this, item);
    }


    // --------------------
    // UI
    // --------------------

    /**
     * Methode permettant de desactiver les champs de saisi en cas de covoiturage complet
     */
    private void updateUI() {
        if (Integer.parseInt(covoiturage.getNbPlacesDispo()) == 0) {
            mTitreInscription.setEnabled(false);
        }
    }

    /**
     * Methode permettant d'afficher les informations de l'user sur l'ecran AccountCreateActivity lorsqu'un user vient de creer un compte
     */
    private void updateUIWhenCreating() {

        if (this.getCurrentUser() != null) { // retourne un user FirebaseUser. Permettra de remplir toutes les vues de l'activité
            getAndShowDatas();
            updateUI();
        }
    }

    /**
     * Methode permettant d'afficher les informations d'un user depuis la bdd firestore lorsque le cycle de vie de l'application est à OnResume()
     */
    private void updateUIWhenResuming() {

        if (this.getCurrentUser() != null) { // retourne un user FirebaseUser. Permettra de remplir toutes les vues de l'activité
            getAndShowDatas();
            updateUI();
        }
    }

    /**
     * Methode permettant à l'utilisateur d'etre redirigé vers la pages principale des covoiturages
     */
    private void startActivityCovoiturageVehicule() {
        Intent intent = new Intent(CovoituragePassagersActivity.this, CovoiturageVehiclesActivity.class);
        startActivity(intent);
    }

    /**
     * Methode permettant de signaler une erreur lorsqu'un champ de nom de passager est resté vide
     * alors que la soumission du formulaire a été faite.
     */
    private boolean verificationChampsVides() {
        //int i = 0;
        if(!mNbPassagerInput.getText().toString().equals(""))
            /*while(i < listNamePassengers.size()){
                if (listNamePassengers.get(i).getText().toString().equals("")) {
                    listNamePassengers.get(i).setError("Merci de renseigner ce champ !");
                    i++;
                }
                return false;
            }*/
        for (int i = 0; i < listNamePassengers.size(); i++) {
            if (listNamePassengers.get(i).getText().toString().equals("")) {
                listNamePassengers.get(i).setError("Merci de renseigner ce champ !");
                listNamePassengers.get(i).requestFocus();
                return false;
            }
        }
        else {
            mNbPassagerInput.setError("Merci de renseigner ce champ !");
            mNbPassagerInput.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Methode permettant d'afficher dynamiquement le nb de champ saisi par l'utilisateur correspondant au nb
     * de passager voulu pour saisir le nom de chacun des passagers
     */
    private void showFieldsNamePassengers(CharSequence charSequence) {
        if (!mNbPassagerInput.getText().toString().equals("")) {
            // empecher un user de demander trop de places en fonction des places dispos
            if (calculNbPlacesRestantes() < 0) {
                final AlertDialog.Builder adb = new AlertDialog.Builder(CovoituragePassagersActivity.this);
                adb.setTitle(R.string.rectif_demande);
                // ajouter une couleur à l'icon de warning
                Drawable warning = getResources().getDrawable(android.R.drawable.ic_dialog_alert);
                ColorFilter filter = new LightingColorFilter( Color.RED, Color.BLUE);
                warning.setColorFilter(filter);
                adb.setIcon(warning);
                adb.setMessage(R.string.alertDialog_places_restantes);
                adb.setPositiveButton("MODIFIER", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mNbPassagerInput.setText("");
                    }
                });
                adb.show();
            } else {
                // condition de creation des champs nom passager dynamique
                if (!mNbPassagerInput.getText().toString().equals("")) {
                    mTitrePassager.setVisibility(View.VISIBLE);
                    inputs = Integer.parseInt(charSequence.toString());
                    if (inputs > 0) {
                        // creation des champs nom passager dynamiquement
                        for (int i = 0; i < inputs; i++) {

                            mFieldNamePassengers = new TextInputEditText(this);
                            mFieldNamePassengers.setHint("Saisir le nom du passager");
                            mFieldNamePassengers.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);

                            mLinearChampsDynamiques.addView(mFieldNamePassengers);
                            listNamePassengers.add(i, mFieldNamePassengers);
                        }
                    }
                }
            }
        } else {
            mTitrePassager.setVisibility(View.GONE);
            mLinearChampsDynamiques.removeAllViews();
            listNamePassengers.clear();
        }
    }

// --------------------
    // ALARM NOTIFICATION
    // --------------------

    /**
     * Methode permettant de generer une alarm dans le systeme du telephone de maniere à envoyer une notification à l'utilisateur
     * 2 heures avant que le covoiturage aller parte.
     */
    private void alarmDepart() {

        Calendar calendar = Calendar.getInstance();

        calendar.setTime(covoiturage.getHoraireAller());
        calendar.add(Calendar.HOUR, -2);
        Intent intent = new Intent(this, TimeAlarmCovoiturageAller.class).putExtra("hAller", String.valueOf(covoiturage.getHoraireAller()));
        PendingIntent operation = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        // reveil de l'alarm
        mAlarmManagerAller.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), operation);
    }

    /**
     * Methode permettant de generer une alarm dans le systeme du telephone de maniere à envoyer une notification à l'utilisateur
     * 2 heures avant que le covoiturage retour parte.
     */
    private void alarmRetour() {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(covoiturage.getHoraireRetour());
        calendar.add(Calendar.HOUR, -2);
        Intent intent1 = new Intent(this, TimeAlarmCovoiturageRetour.class).putExtra("hRetour", String.valueOf(covoiturage.getHoraireRetour()));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent1, PendingIntent.FLAG_ONE_SHOT);
        // reveil de l'alarm
        mAlarmManagerRetour.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    // --------------------
    // REST REQUETES
    // --------------------

    /**
     * Methode permettant de recuperer et d'afficher toutes les informations d'un covoiturage
     */
    private void getAndShowDatas() {
        mIntent = getIntent();
        covoiturage = (Covoiturage) Objects.requireNonNull(mIntent.getExtras()).getSerializable("covoit");
        assert covoiturage != null;

        mNomConducteur.setText(Html.fromHtml("<b>Conducteur : </b>" + covoiturage.getPrenomConducteur() + " " + covoiturage.getNomConducteur()));
        mDateDepart.setText(Html.fromHtml("<b>Aller : départ le </b>" + stDateToString(covoiturage.getHoraireAller()) + "<b> depuis </b>" + covoiturage.getLieuDepartAller()));
        mDateretour.setText(Html.fromHtml("<b>Retour : départ le </b>" + stDateToString(covoiturage.getHoraireRetour()) + "<b> jusqu'à </b>" + covoiturage.getLieuDepartRetour()));
        mNbPlaceDispo.setText(Html.fromHtml("<b>Places disponibles : </b>" + covoiturage.getNbPlacesDispo() + " / " + covoiturage.getNbPlacesTotal()));
        mTypeVehicule.setText(Html.fromHtml("<b>Type Véhicule : </b>" + covoiturage.getTypeVehicule()));
    }

    /**
     * Methode permettant la creation d'un user dans le bdd. En cas d'insertion ou de probleme,
     * la fonction renverra une notification à l'utilisateur.
     */
    private void createPassagerInCovoiturage() {

        int nbPlacesRestantes = calculNbPlacesRestantes();

        // empecher un user de demander trop de places en fonction des places dispos
        if (nbPlacesRestantes < 0 && listNamePassengers.size() > Integer.parseInt(covoiturage.getNbPlacesDispo())) {

            final AlertDialog.Builder adb = new AlertDialog.Builder(CovoituragePassagersActivity.this);
            adb.setTitle(R.string.rectif_demande);
            // ajouter une couleur à l'icon de warning
            Drawable warning = getResources().getDrawable(android.R.drawable.ic_dialog_alert);
            ColorFilter filter = new LightingColorFilter( Color.RED, Color.BLUE);
            warning.setColorFilter(filter);
            adb.setIcon(warning);
            adb.setMessage(R.string.alertDialog_places_restantes);
            adb.setPositiveButton("MODIFIER", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mNbPassagerInput.setText("");
                }
            });
            adb.show();

            // si le nb de places demandées est bon, on insere tous les noms des passagers dans la bdd
        } else {
            if (verificationChampsVides()) {
                String placesRestantes = String.valueOf(nbPlacesRestantes);
                mNbPlaceDispo.setText(placesRestantes);
                List<String> listPassagers = new ArrayList<>();
                listPassagers.addAll(covoiturage.getListPassagers());

                // ajout des infos du passager dans l'objet covoiturage
                for (int i = 0; i < listNamePassengers.size(); i++) {
                    listPassagers.add(listNamePassengers.get(i).getText().toString().toUpperCase());
                    // notification d'alarme à chacun des passagers : mais il faut que le nom rentré correspondent à quelque chose!
                    this.alarmDepart();
                    this.alarmRetour();
                }

                this.mProgressBar.setVisibility(View.VISIBLE);
                //CRUD
                CovoiturageHelper.updateCovoiturage(covoiturage.getId(), placesRestantes, listPassagers)
                        .addOnFailureListener(this.onFailureListener())
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(CovoituragePassagersActivity.this, R.string.create_passager,
                                        Toast.LENGTH_SHORT).show();
                                startActivityCovoiturageVehicule(); // renvoi l'user sur la page des covoiturages apres validation de la creation de l'user dans les covoit
                            }
                        });
            }
        }
    }

    /**
     * Methode de calcul du nombre de places restantes dans un covoiturage
     *
     * @return
     */
    private int calculNbPlacesRestantes() {
        String passagers = mNbPassagerInput.getText().toString();
        int nbPassagers = 0;
        if(!passagers.equals("")) {
            nbPassagers = Integer.parseInt(passagers);
        }
            int nbPlacesDispo = Integer.parseInt(covoiturage.getNbPlacesDispo());
            int nbPlacesRestantes = nbPlacesDispo - nbPassagers;

        return nbPlacesRestantes;
    }
}
