# Organisator — suivi d'impressions 3D

Application Android de suivi d'impression 3D **résine** (SLA / MSLA / DLP) qui
remplace le fichier Excel d'atelier : temps, coûts, fichiers imprimés, mémos,
résultat des plateaux, statistiques et rappels.

## Ce que fait l'application

**Projets** en écran d'accueil — la liste sert de menu : photo à gauche, nom et
avancement à droite, un appui ouvre le détail et ses plateaux.

**Plateaux** — l'unité de suivi. Chaque plateau porte son nom, le fichier
imprimé, l'imprimante choisie dans le parc (M7 Max, M7 Pro Gris Gauche,
M7 Pro Gris Droite, M7 Pro Clear), le type de résine et sa couleur, la hauteur de couche,
l'échelle, le nombre d'exemplaires, le temps estimé et le temps réel, le volume
de résine consommé et ses coûts annexes, plus un mémo libre.

**Statuts** — `À faire`, `À imprimer`, `En cours`, `Terminé`, `À refaire`,
`Annulé`. Un plateau `En cours` affiche sa progression et le temps restant, mis
à jour en continu.

**Résultat du plateau** — on note si tout le plateau est sorti correctement ou
s'il reste des pièces à refaire.

**Pièces** — à la création d'un plateau, une saisie libre (« tête bras jambes »)
devient une pièce par mot, les espaces servant de séparateur. Chaque pièce
s'affiche ensuite comme un bouton sur la fiche du plateau : un appui la passe en
rouge, ce qui la range dans les pièces à refaire du projet ; un appui long la
retire.

**Entretien** — un journal par imprimante : film FEP, écran LCD, nettoyage,
calibration, plateau, bidon de résine. Chaque intervention porte sa date, une
remarque et le compteur de couches relevé ce jour-là, le meilleur repère d'usure.

**Projets** — regroupent plusieurs plateaux, avec avancement, temps et coût
cumulés, photo, couleur de repérage et échéance. La photo est choisie dans la
galerie puis recopiée, redimensionnée, dans le stockage privé de l'application :
elle reste lisible après un redémarrage et suit la sauvegarde du téléphone. La liste des plateaux se filtre par
projet et se regroupe par projet, avec le total de chaque section.

**Statistiques** — échelle de temps au choix (7 j, 30 j, 90 j, 1 an, tout) :
temps d'impression, coût, nombre de plateaux, résine consommée, taux de
réussite, histogramme par jour / semaine / mois, répartition par statut,
consommation par type de résine, classement des projets.

**Coûts** — résine (millilitres × prix au litre), électricité (puissance × durée
× prix du kWh), coût machine horaire optionnel, consommables de lavage et de
durcissement par plateau, et coûts annexes. Le détail est affiché poste par
poste sur chaque plateau.

**Rappels** — une notification à l'heure choisie pour lancer un plateau. Les
rappels sont réarmés automatiquement après un redémarrage du téléphone.

**Raccourcis et liens** — un appui long sur l'icône propose « Nouveau plateau »
et « Nouveau projet », qu'on peut déposer sur l'écran d'accueil. Les mêmes
écrans s'ouvrent par lien profond :

```
organisator://plateau/nouveau
organisator://projet/nouveau
```

De quoi déclencher la saisie depuis une routine vocale de l'assistant, un
bouton d'écran d'accueil ou une application d'automatisation, sans que
l'application ait besoin d'écouter le micro.

## Installer

Téléchargez l'APK depuis la page
[Releases](https://github.com/Alizee-M/Organisator/releases), ouvrez-le sur le
téléphone et autorisez l'installation depuis cette source. Android 8.0 minimum.

À l'ouverture, acceptez les notifications. Pour que les rappels tombent à
l'heure exacte, autorisez aussi les « alarmes et rappels » depuis
Réglages → Notifications dans l'application.

## Compiler soi-même

```bash
./gradlew assembleRelease
```

L'APK est produit dans `app/build/outputs/apk/release/`.

Le dépôt contient une clé de signature de sideload
(`keystore/organisator-sideload.jks`, mot de passe `organisator`). Elle n'est pas
un secret : elle sert uniquement à ce que les mises à jour s'installent par-dessus
la version précédente. Pour signer avec votre propre clé, définissez les secrets
de dépôt `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`,
`ANDROID_KEY_ALIAS` et `ANDROID_KEY_PASSWORD` : le workflow les utilisera à la
place.

## Publier une version

Mettez à jour `version.properties`, puis au choix :

- poussez un tag `git tag v1.0.1 && git push origin v1.0.1` ;
- ou lancez le workflow « Build APK » depuis l'onglet Actions en cochant
  « Publier une release GitHub avec l'APK » : le tag est alors déduit de
  `version.properties`.

Dans les deux cas le workflow compile l'APK et crée la release avec le fichier
attaché.

## Architecture

- **Kotlin + Jetpack Compose**, Material 3, thème clair ou sombre au choix
  (Système, Clair, Sombre) depuis les réglages.
- **Room** pour la base locale (`projects`, `print_jobs`, `print_parts`), avec
  migration versionnée.
- **AlarmManager** pour les rappels, réarmés au boot.
- Graphiques dessinés au `Canvas`, sans dépendance externe.
- Photos de projet recopiées dans `files/project_photos`, décodées hors du fil
  principal ; aucune permission de stockage n'est demandée.
- Tout reste sur l'appareil : aucun compte, aucun réseau.

```
app/src/main/java/com/organisator/print3d/
├── data/          entités Room, DAO, calculs de coût, moteur de statistiques
├── notifications/ planification des alarmes et affichage des rappels
├── ui/
│   ├── components/ cartes, puces, graphiques, champs de formulaire
│   ├── screens/    tableau de bord, plateaux, projets, stats, réglages
│   └── theme/      palette et typographie
└── util/          formatage des dates, durées, montants
```
