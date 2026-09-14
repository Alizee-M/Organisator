# Organisator — suivi d'impressions 3D

Application Android qui remplace le fichier Excel de suivi d'atelier : temps,
coûts, fichiers imprimés, mémos, résultat des plateaux, statistiques et rappels.

## Ce que fait l'application

**Plateaux** — l'unité de suivi. Chaque plateau porte son nom, le fichier
imprimé, l'imprimante, la matière et la couleur, l'échelle, le nombre
d'exemplaires, le temps estimé et le temps réel, le filament consommé et ses
coûts annexes, plus un mémo libre.

**Statuts** — `À faire`, `À imprimer`, `En cours`, `Terminé`, `À refaire`,
`Annulé`. Un plateau `En cours` affiche sa progression et le temps restant, mis
à jour en continu.

**Résultat du plateau** — on note si tout le plateau est sorti correctement ou
s'il reste des pièces à refaire. Chaque pièce du plateau peut être listée et
suivie individuellement (`À faire` → `Imprimée` → `À refaire`).

**Projets** — regroupent plusieurs plateaux, avec avancement, temps et coût
cumulés, couleur de repérage et échéance.

**Statistiques** — échelle de temps au choix (7 j, 30 j, 90 j, 1 an, tout) :
temps d'impression, coût, nombre de plateaux, filament consommé, taux de
réussite, histogramme par jour / semaine / mois, répartition par statut,
consommation par matière, classement des projets.

**Coûts** — filament (grammes × prix/kg), électricité (puissance × durée × prix
du kWh), coût machine horaire optionnel, et coûts annexes. Le détail est affiché
poste par poste sur chaque plateau.

**Rappels** — une notification à l'heure choisie pour lancer un plateau. Les
rappels sont réarmés automatiquement après un redémarrage du téléphone.

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

Mettez à jour `version.properties`, puis poussez un tag :

```bash
git tag v1.0.0 && git push origin v1.0.0
```

Le workflow compile l'APK et crée la release GitHub avec le fichier attaché.

## Architecture

- **Kotlin + Jetpack Compose**, Material 3, thème clair/sombre sobre.
- **Room** pour la base locale (`projects`, `print_jobs`, `print_parts`).
- **AlarmManager** pour les rappels, réarmés au boot.
- Graphiques dessinés au `Canvas`, sans dépendance externe.
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
