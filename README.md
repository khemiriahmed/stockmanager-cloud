# StockManager Cloud

Application web de gestion de stock, de commandes clients et de facturation. Le projet est un monolithe en couches, livré avec une API REST sécurisée, une interface Angular et une orchestration Docker prête pour le cloud.

## Sommaire

- [Fonctionnalités](#fonctionnalités)
- [Stack technique](#stack-technique)
- [Architecture](#architecture)
- [Démarrage rapide avec Docker](#démarrage-rapide-avec-docker)
- [Développement local](#développement-local)
- [Comptes et rôles](#comptes-et-rôles)
- [Cycle de vie des commandes et du stock](#cycle-de-vie-des-commandes-et-du-stock)
- [API et documentation](#api-et-documentation)
- [Tests](#tests)
- [Variables d'environnement](#variables-denvironnement)
- [CI/CD et déploiement AWS](#cicd-et-déploiement-aws)
- [Structure du projet](#structure-du-projet)

## Fonctionnalités

- **Authentification JWT** : connexion, profil, changement de mot de passe, rôles `ADMIN`, `MANAGER`, `EMPLOYEE`.
- **Catalogue** : produits, catégories, fournisseurs, prix d'achat/vente, seuils d'alerte, activation/désactivation.
- **Clients et entrepôts** : fiches complètes, coordonnées, responsables, statuts.
- **Stocks multi-entrepôts** : consultation, réservations, entrées, sorties, transferts atomiques, ajustements justifiés.
- **Alertes** : produits sous le seuil et ruptures de stock.
- **Commandes** : création en brouillon, réservation du stock à la confirmation, préparation, expédition, livraison, annulation.
- **Facturation** : génération automatique d'une facture PDF à la livraison, téléchargement.
- **Tableau de bord** : indicateurs temps réel, chiffre d'affaires sur 12 mois, pipeline de commandes, alertes.
- **Traçabilité** : historique des mouvements avec opérateur, motif, date et entrepôts concernés.
- **Robustesse** : migrations Flyway, erreurs RFC 7807, verrous pessimistes, tests d'intégration Testcontainers.

## Stack technique

| Couche | Technologies |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, Flyway |
| Base de données | PostgreSQL 16 |
| Authentification | JWT (JJWT), BCrypt |
| Documentation API | OpenAPI 3 / Swagger UI |
| PDF | OpenPDF |
| Stockage documents | Système de fichiers local ou AWS S3 |
| Frontend | Angular 17, Angular Material 17, TypeScript, Chart.js |
| Tests | JUnit 5, Mockito, Testcontainers |
| Conteneurs | Docker, Docker Compose, nginx |
| CI/CD | GitHub Actions |

## Architecture

```text
Navigateur
   │
   ▼
Angular 17 (nginx)
   │  /api/v1 (proxy)
   ▼
Spring Boot 3
   ├── Spring Security + JWT
   ├── Contrôleurs REST + validation
   ├── Services métier transactionnels
   ├── Repositories Spring Data JPA
   ├── Flyway (schéma versionné)
   ├── PostgreSQL 16
   └── Stockage PDF (local ou S3)
```

Le backend est organisé par domaine fonctionnel (`auth`, `user`, `product`, `stock`, `order`, `invoice`, etc.), avec des DTO exposés par l'API et des exceptions métier traduites en réponses Problem Details.

## Démarrage rapide avec Docker

Prérequis : Docker Desktop avec Docker Compose.

```bash
cp .env.example .env
# Adapter au minimum POSTGRES_PASSWORD, DB_PASSWORD et JWT_SECRET
docker compose up --build
```

Services disponibles :

| Service | URL |
| --- | --- |
| Application | http://localhost:8081 |
| API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| PostgreSQL | localhost:5432 |

Les données PostgreSQL et les PDF sont conservés dans les volumes `postgres-data` et `documents-storage`.

## Développement local

### Prérequis

- JDK 21
- Maven 3.9+
- Node.js 20+ et npm
- PostgreSQL 16 (ou le conteneur `postgres` de Docker Compose)

### Base de données

```bash
docker compose up -d postgres
```

### Backend

```bash
cd backend
mvn spring-boot:run
```

Variables utiles si les valeurs par défaut ne conviennent pas :

```bash
DB_URL=jdbc:postgresql://localhost:5432/stockmanager
DB_USERNAME=stockmanager
DB_PASSWORD=stockmanager
JWT_SECRET=une-valeur-aleatoire-de-32-caracteres-minimum
SEED_DEMO_DATA=true
```

### Frontend

```bash
cd frontend
npm install
npm start
```

Le serveur de développement écoute sur http://localhost:4200 et proxyfie `/api` vers `http://localhost:8080` (`proxy.conf.json`).

## Comptes et rôles

| Rôle | Droits principaux |
| --- | --- |
| `ADMIN` | Tous les droits, gestion des utilisateurs et des rôles |
| `MANAGER` | Catalogue, stocks avancés, commandes, livraison, facturation, consultation des utilisateurs |
| `EMPLOYEE` | Consultation, entrées/sorties de stock, création et suivi des commandes |

Comptes créés au démarrage :

- Administrateur : défini par `ADMIN_EMAIL` / `ADMIN_PASSWORD` (valeurs par défaut : `admin@stockmanager.local` / `Admin@12345`).
- Données de démonstration (`SEED_DEMO_DATA=true`) :
  - `manager@stockmanager.local` / `Manager@12345`
  - `employee@stockmanager.local` / `Employee@12345`

> Changez les mots de passe et le secret JWT avant tout déploiement public.

## Cycle de vie des commandes et du stock

```text
DRAFT ──confirm──▶ CONFIRMED ──process──▶ PROCESSING ──ship──▶ SHIPPED ──deliver──▶ DELIVERED
  │                    │                      │
  └──────cancel────────┴──────cancel──────────┘
```

- `DRAFT` → `CONFIRMED` : le stock disponible est réservé par ligne de commande.
- `CONFIRMED` → `PROCESSING` : préparation logistique.
- `PROCESSING` ou `CONFIRMED` → `SHIPPED` : décrément physique du stock et libération de la réservation.
- `SHIPPED` → `DELIVERED` : génération de la facture PDF.
- `CANCELLED` : libération des réservations si la commande était confirmée ou en préparation.

Les entrées, sorties, transferts et ajustements sont enregistrés dans l'historique des mouvements. Les opérations sensibles utilisent des transactions et des verrous pessimistes pour éviter les surventes.

## API et documentation

Swagger UI : http://localhost:8080/swagger-ui.html

Principaux préfixes :

| Préfixe | Contenu |
| --- | --- |
| `/api/v1/auth` | Connexion JWT |
| `/api/v1/users` | Profil et administration des utilisateurs |
| `/api/v1/products` | Catalogue produits |
| `/api/v1/categories` | Catégories |
| `/api/v1/suppliers` | Fournisseurs |
| `/api/v1/customers` | Clients |
| `/api/v1/warehouses` | Entrepôts |
| `/api/v1/stocks` | Niveaux, alertes, mouvements, opérations |
| `/api/v1/orders` | Commandes et transitions de statut |
| `/api/v1/invoices` | Factures et PDF |
| `/api/v1/dashboard` | Indicateurs de pilotage |

Exemple de connexion :

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"manager@stockmanager.local","password":"Manager@12345"}'
```

Les erreurs suivent le format Problem Details (RFC 7807) avec un code métier (`code`) et, pour les validations, un objet `errors`.

## Tests

### Backend

```bash
cd backend
mvn test
```

Le test d'intégration utilise Testcontainers et démarre un PostgreSQL 16 éphémère. Il est automatiquement ignoré si Docker n'est pas disponible.

```bash
mvn -Dtest=StockManagerIntegrationTest test
```

Couverture fonctionnelle des tests :

- réservation, sortie et transfert de stock ;
- refus de stock insuffisant ;
- transitions de commande et génération de facture PDF ;
- authentification et contrôle des permissions.

### Frontend

```bash
cd frontend
npm run build
```

## Variables d'environnement

Voir `.env.example`. Les principales :

| Variable | Rôle | Défaut local |
| --- | --- | --- |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | Base PostgreSQL | `stockmanager` / `stockmanager` / `stockmanager` |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Connexion JDBC du backend | dérivés de PostgreSQL |
| `JWT_SECRET` | Secret de signature JWT (32+ caractères) | secret de développement |
| `JWT_EXPIRATION_MINUTES` | Durée des tokens | `480` |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Compte administrateur initial | `admin@stockmanager.local` / `Admin@12345` |
| `SEED_DEMO_DATA` | Données de démonstration | `false` (local), `true` (Docker) |
| `STORAGE_TYPE` | `local` ou `s3` | `local` |
| `S3_BUCKET_DOCUMENTS` / `AWS_REGION` | Stockage S3 des PDF | vide / `eu-west-3` |
| `CORS_ALLOWED_ORIGINS` | Origines autorisées | `http://localhost:4200,http://localhost:8081` |
| `FRONTEND_PORT` / `BACKEND_PORT` | Ports publiés | `8081` / `8080` |

## CI/CD et déploiement AWS

Le workflow `.github/workflows/ci.yml` exécute à chaque push/PR :

1. tests et packaging backend (`mvn verify`) ;
2. build de production Angular ;
3. construction des images Docker backend et frontend (cache Buildx).

Pistes de déploiement cloud :

- pousser les images vers Amazon ECR ;
- exécuter les conteneurs sur ECS Fargate ou EKS ;
- utiliser RDS PostgreSQL avec les migrations Flyway au démarrage ;
- stocker les PDF dans S3 (`STORAGE_TYPE=s3`) ;
- injecter les secrets via AWS Secrets Manager / SSM Parameter Store ;
- placer l'application derrière un Application Load Balancer avec HTTPS.

Le backend est déjà configuré pour fonctionner sans modification dans un conteneur (`application-docker.yml`, healthcheck Actuator, stockage `/app/storage/documents`).

## Structure du projet

```text
stockmanager-cloud/
├── .github/workflows/ci.yml
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/stockmanager/   # domaines métier
│       ├── main/resources/db/migration/  # Flyway
│       └── test/java/com/stockmanager/   # tests unitaires et intégration
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/app/
│       ├── core/                         # services, guards, interceptors, modèles
│       ├── layout/                       # shell authentifié
│       ├── shared/                       # composants réutilisables
│       └── features/                     # pages métier
├── docs/
├── docker-compose.yml
└── .env.example
```
