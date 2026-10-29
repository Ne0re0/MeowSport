# MeowSport 🐱💪

Application Android de suivi de musculation, 100 % hors ligne.

## Fonctionnalités

- **Modèles de séance** : choisis tes exercices dans un catalogue de plus de 150 exercices de salle (recherche en anglais ou en français, filtre par muscle), ou crée les tiens.
- Pour chaque exercice : poids (avec option « par main »), séries, répétitions, repos entre les séries et repos avant l'exercice suivant.
- **Séance en direct** : chronomètre global, séries à cocher, poids et répétitions modifiables, minuteur de récupération lancé automatiquement (vibration et bip à la fin). L'écran reste allumé.
- **Notification de séance** : pendant toute la séance, une notification affiche le chrono et le repos en cours, avec Pause / Reprendre et « Terminer et sauvegarder ». Le chrono se met aussi en pause depuis l'écran de séance.
- **Bip de fin de repos** : « tut, tut, tuuut » pendant les 3 dernières secondes, en baissant le son des autres applis, puis vibration.
- **Fin de séance** : le modèle est mis à jour avec ce que tu as fait. Si tu as fait mieux (plus lourd, plus de répétitions ou plus de volume), tu es félicité.
- **Notes** : une note pour la prochaine fois sur la séance, et une note par exercice.
- **Profil** : séances et temps passé cette semaine, objectif hebdomadaire, graphique sur 8 semaines, historique détaillé.
- **Niveau par muscle** (Profil) : une carte du corps, face et dos, colorée du bleu clair (débutant) au noir (expert). Le niveau vient de ta meilleure série sur chaque exercice : 1RM estimé (Epley) rapporté à ton poids de corps et comparé à des standards de force. Renseigne ton poids dans le profil pour l'activer.
- **Import / export** (menu ⋮ de l'écran Profil) : sauvegarde tes modèles et ton historique dans un fichier JSON, puis réimporte-le sur un autre téléphone. Réimporter le même fichier n'ajoute pas de doublons.
- Chaque exercice a une illustration du corps avec le muscle travaillé en couleur. Ces dessins sont faits pour l'app, sans image tierce.

## Installer l'APK

Chaque mise à jour de `main` compile l'APK et le publie dans les **Releases** du dépôt (`MeowSport.apk`).
Ouvre la page Releases depuis ton téléphone, télécharge l'APK et autorise l'installation depuis ton navigateur.
Toutes les versions sont signées avec la même clé (`keystore/`), donc une nouvelle version s'installe par-dessus l'ancienne sans perdre tes données.

## Technique

Kotlin, Jetpack Compose (Material 3), Room. minSdk 26 (Android 8.0).

Compiler en local : `./gradlew assembleRelease` (il faut le SDK Android).
