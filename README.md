# GuardianTrack

**Application de Sécurité Personnelle Avancée**

Ce projet est réalisé dans le cadre du Mini-Projet Évalué pour la 1ère année Master Professionnel en Développement d'Applications Mobiles (Cours Développement Android Natif avancé (Kotlin)).

## Objectifs Pédagogiques Atteints
- **Architecture Moderne** : Respect rigoureux du pattern **MVVM** avec Clean Architecture (Domain, Data, UI).
- **Les 4 Piliers Android** :
  - **Activity & Fragment (Compose)** : Single Activity avec Navigation Compose (Remplacement de ViewBinding par Jetpack Compose pour le bonus de +5).
  - **Foreground Service** : Surveillance des capteurs (Accéléromètre) en continu en arrière-plan.
  - **BroadcastReceivers** : Gestion de `ACTION_BOOT_COMPLETED` (avec repli `WorkManager` pour Android 12+) et `ACTION_BATTERY_LOW`.
  - **ContentProvider** : Partage sécurisé des contacts d'urgence.
- **Persistance et Synchronisation** :
  - Stockage local avec **Room** (Base de données) et **DataStore** avec **EncryptedSharedPreferences** pour la sécurité.
  - Stratégie Offline-First pour la synchronisation distante avec l'API (**Retrofit** + **WorkManager**).
  - Export CSV vers `Documents/` via `MediaStore` (Scoped Storage).
- **Design Original** : Thème exclusif "Cybersecurity" (Dark-first, néon, composants uniques, animation d'impulsion).
- **Hilt** : Injection de dépendances entièrement managée.

## Configuration du Build
L'application cible **Android API 26 minimum** (Android 8.0) jusqu'à l'**API 34**.

### Pré-requis : `local.properties`
L'application s'attend à trouver l'URL de base de l'API dans le fichier `local.properties`. Celui-ci a été intentionnellement ajouté au `.gitignore`. Pour builder et exécuter, vous **devez** ajouter cette ligne à votre `local.properties` :
```properties
API_BASE_URL=https://6625e40e052332d21d10e523.mockapi.io/api/v1/
```
(Remplacez par votre propre URL mockapi ou Firebase si nécessaire)

## Design et Interface
L'application propose un **design unique et exclusif**, très différent des autres projets :
- Cartes **Glassmorphism** avec bords semi-transparents.
- **Micro-animations** (bouton SOS pulsant, bouclier animé).
- **Mode Simulation SMS** (activé par défaut comme demandé) clairement signalé par un badge dans les paramètres.

## Réponses Techniques (Rapport)
Le rapport technique complet et argumenté répondant aux 6 questions obligatoires se trouve dans le fichier PDF fourni séparément, ainsi que le diagramme de l'architecture.

## Compatibilité Android 12/13/14
- Permissions post-notifications dynamiques (`Android 13`).
- Démarrage des Services via Broadcast (`Android 12`) : Un `BootReceiver` intercepte l'événement, mais l'utilisation directe du service (bloquée dans API 31+) est renforcée par un fall-back propre et sûr avec un **Expedited WorkRequest** de WorkManager (`ServiceRestartWorker`).
- `ForegroundServiceType` défini (Android 14) : Le type est défini sur `location` pour le `SurveillanceService`.
