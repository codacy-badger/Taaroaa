package fr.drochon.christian.taaroaa.auth;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import fr.drochon.christian.taaroaa.R;
import fr.drochon.christian.taaroaa.model.User;

public class SearchedUserViewHolder extends RecyclerView.ViewHolder {

    //DATA
    private final List<User> mSearchedUserList;
    private TextView mNomSearched;
    private TextView mPrenomSearched;
    private TextView mEmailSearched;

    /**
     * Contructeur qui prend en param la vue affichée.
     * Je recupere les 2 textview du layout pupils_cell.
     * responsable du clic sur les cellules.
     *
     * @param itemView : cellule d'une liste comprenant le nom prenom et email de la personne recherchée.
     */
    @SuppressLint("CutPasteId")
    SearchedUserViewHolder(View itemView) {
        super(itemView);

        mEmailSearched = itemView.findViewById(R.id.list_cell_email);
        mNomSearched = itemView.findViewById(R.id.liste_cell_nom);
        mPrenomSearched = itemView.findViewById(R.id.list_cell_prenom);

        mSearchedUserList = new ArrayList<>();

        // --------------------
        // LISTENER
        // --------------------

        // Affichage du contenu de la cellule
        // j'utilise l'ecouteur sur la cellule et recupere les informations pour les affihcer dans une notification
        // j'envoie un intent vers la classe ModificationAccount pour que cette classe sache quel utilisateur a été selectionné par l'encadrant
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (User u : mSearchedUserList
                        ) {
                    Intent intent = new Intent(v.getContext(), AccountModificationActivity.class).putExtra("searchedUser", u);
                    v.getContext().startActivity(intent);
                }
            }
        });
    }

    /**
     * Methode appellée via l'adapter. Cette methode mettra à jour les differentes view du viewholder en fonction de l'utilisateur connecté.
     * Toutes les personnes de la bdd seront affichées excepté les moniteurs. Un encadrant ne pourra donc pas se modifier lui
     * meme puisque son nom ne s'affiche pas dans la liste des rehcerches, ni aucun autres moniteurs. Chaque moniteur gere son compte.
     *
     * @param user utilisateur
     */
    public void updateWithUser(final User user) {

        // ajout des Covoit dans une liste afin de les retrouver pour l'affichage de chaque cours particulier sous forme de notification
        mSearchedUserList.add(user);

        for (int i = 0; i < mSearchedUserList.size(); i++) {
            if (user.getFonction().equals("Moniteur")) {
                mPrenomSearched.setVisibility(View.GONE);
                mNomSearched.setVisibility(View.GONE);
                mEmailSearched.setVisibility(View.GONE);
            }
            mPrenomSearched.setText(user.getPrenom());
            mNomSearched.setText(user.getNom());
            mEmailSearched.setText(user.getEmail());
        }
    }
}
