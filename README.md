# IUT-2025-2026_SAE-302

Application locale permettant de **consulter, filtrer et rechercher des CVE Web** depuis une base SQLite.  
Elle contient **une API FastAPI (interface machine / Android / interface utilisateur)** via `uvicorn` et une application Java via `picocli` et `JLine`.

## Sommaire

- [Introduction](#iut-2025-2026_sae-302)
- [🚀 Fonctionnalités](#-fonctionnalités)
- [📂 Architecture](#-architecture)
  - [📂 Web](#-architecture-web)
  - [📂 CLI](#-architecture-cli-app-java)
- [🔧 Installation](#-installation)
  - [🐍💻 1. Installer Python](#-1-installer-python)
  - [🫖💻 2. Installer Java 21 (jdk)](#-2-installer-java-21-jdk)
  - [🛠️🌐 3. Créer un environnement virtuel](#️-3-créer-un-environnement-virtuel)
  - [🔧📥 4. Cloner le dépôt Git](#-4-cloner-le-dépôt-git)
  - [🔧🔧 5. Mettre en place l'environnement](#-5-mettre-en-place-lenvironnement)
  - [📦🚀 6. Installer les dépendances python](#-6-installer-les-dépendances-python)
- [🌍 Utilisation & Installation du Site Web](#-utilisation-du-site-web)
  - [▶️ Lancement](#️-lancement)
  - [🧑‍💻 Accès](#-accès)
- [🫖 Utilisation de l'application Java](#-utilisation-de-lapplication-java)
- [🗃️ Base de données](#️-base-de-données)
- [🔌 API — Endpoints principaux](#-api--endpoints-principaux)
- [🎯 Objectif](#-objectif)
- [📝 Autres](#-autres)

---

## 🚀 Fonctionnalités

- ✅ Lecture des CVE Web depuis une base SQLite
- ✅ Tableau dynamique (recherche, tri, filtrage, pagination côté serveur)
- ✅ Aucune authentification (usage LAN / local only)
- ✅ API REST standard en lecture seule
- ✅ Compatible téléphone / émulateur Android
- ✅ Lancement simple (application java bshell)
- ✅ Implémentation d'une détection de différences dans les CVE Web (open/closed) par l'application Java
- ✅Implémentation d'une gestion d'outils dans l'application Java sous forme de module

---

## 📂 Architecture

### 📂 Architecture Web

```
main.py             # Lance FastAPI avec API REST (/v1/…) + Site web Flask (/failles)
|
Web_app/
├─ database.py      # Code Python accès SQLite, gestion filtre/recherche etc...
├─ dev.db           # Base SQLite locale
│
├─ templates/
│   ├─ base.html
│   ├─ home.html
│   ├─ states.html
│   └─ failles.html
└─ static/
    └─ css/
        └─ style.css
    └─ favicon.ico
```

---

### 📂 Architecture CLI (App Java)

```
CLI/
|
BShell/ 
├─ gradle/
├─ gradlew
└─ bshell/
    └─ src/main
        ├─ resources/ASCII
        └─ java/bshell
            ├─ configs/
            |   ├─ Config.java
            |   └─ ConfigManager.java
            ├─ database/
            |   ├─ DatabaseRepository.java
            |   ├─ DatabaseService.java
            |   ├─ SQLiteRepository.java
            |   └─ Vulnerability.java
            ├─ modules/
            |   ├─ AbstractModule.java
            |   ├─ Module.java
            |   ├─ ModuleExecutionException.java
            |   ├─ ModuleManager.java
            |   ├─ NucleiModule.java
            |   └─ Option.java
            ├─ shell/
            |   └─ ShellService.java
            ├─ BShell.java
            └─ Setup.java

```

**Explication :**
- _gradlew_ : C'est le binaire qui automatise le build et la compilation des bibliothéques externe de l'application.
- _BShell_ : C'est le entrypoint du projet Java, il permet aussi de gèrer les intéractions cli.
- _Setup_ : Gestion de l'installation des dépendances et extensions comme Nuclei. Setup du fichier de configs de l'app.
- _Configs/Config_ : Contient le fichier de configs sous un format Objet.
- _Configs/ConfigManager_ : Singleton + gère les intéractions liée au fichier de configs, ex load/save.
- _database/DatabaseRepository_ : Interface, dicte le fonctionnement et les méthodes que doivent implémenter une classe liée à la BdD.
- _database/DatabaseService_ : Singleton, classe permettant d'intéragir avec la BdD.
- _database/SQLiteRepository_ : Code gérant le SQL.
- _database/Vulnerability_ : Objet CVE. (avec surcharge)
- _modules/AbstractModule_ : C'est une classe qui fonctionne comme une template pour l'ajout de nouveaux outils.
- _modules/Module_ : Interface, dicte le fonctionnement et les méthodes que doivent implémenter une classe liée à la BdD.
- _modules/ModuleExecutionException_ : Gère les erreurs customes des modules.
- _modules/ModuleManager_ : Enregistre les modules.
- _modules/NucleiModule_ : Gère l'outil Nuclei.
- _modules/Option_ : Objet Option.
- _shell/ShellService_ : Gère la partie Shell.

---

## 🔧 Installation

### 🐍💻 1. Installer Python

Windows
- Télécharger et installer Python depuis [https://www.python.org/downloads/](https://www.python.org/downloads/)
- Lors de l'installation, cocher l'option **Add Python to PATH**.
- Vérifier l'installation :
```bash
python3 --version
```

macOS
```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
brew install python3
python3 --version
```

Linux (Debian)
```bash
sudo apt update
sudo apt install python3 python3-pip
python3 --version
```

---

### 🫖💻 2. Installer Java 21 (jdk)

À télécharger : https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.deb

Linux (Debian)
```bash
wget https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.deb
sudo dpkg -i jdk-21_linux-x64_bin.deb
```

Si vous rencontrez des problèmes liés au dpkg (ldconfig ou start-stop-daemon), veuillez exécuter les commandes suivantes pour réparer le PATH :
```bash
export PATH=/usr/local/sbin:/usr/local/bin:/usr/bin:/sbin:/bin
```

Puis ré-exécuter les commandes au-dessus.

---

### 🛠️🌐 3. Créer un environnement virtuel

Sur tous les systèmes
- Naviguer dans le dossier du projet ou créer un nouveau dossier :
```bash
mkdir mon_projet
cd mon_projet
```

- Créer l’environnement virtuel :
```bash
python -m venv env
```
ou, si `python` pointe vers Python 2, utiliser :
```bash
python3 -m venv env
```

- Activer l’environnement virtuel :

  - Sur Windows (PowerShell) :
  ```bash
  .\env\Scripts\activate
  ```

  - Sur Windows (CMD) :
  ```bash
  env\Scripts\activate.bat
  ```

  - Sur macOS/Linux :
  ```bash
  source env/bin/activate
  ```

Vérification
- La ligne de commande doit afficher le nom de l’environnement, par exemple `(env)`.

---

### 🔧📥 4. Cloner le dépôt Git

```bash
sudo apt-get install -y git
git clone https://github.com/GombertJ/IUT-2025-2026_SAE-302.git --branch v6.5.0
```

---

### 🔧🔧 5. Mettre en place l'environnement

Selon [FHS](https://en.wikipedia.org/wiki/Filesystem_Hierarchy_Standard), les applications externes (donc ce qui est non paquet) est généralement mis dans /opt

#### 🫖 Java
```bash
cd IUT-2025-2026_SAE-302/
unzip bshell.zip -d /opt/
chmod 077 /opt/bshell
chmod +x /opt/bshell/bin/bshell
ln -s /opt/bshell/bin/bshell /usr/local/bin/bshell
```

_Remarque_ : Ici l'application `bshell` sera accessible comme une commande pour l'utilisateur. 

#### 🐍 Python
```bash
unzip Web.zip -d /opt/bshell/
```

---

### 📦🚀 6. Installer les dépendances python

**Mise en place du venv**:
```bash
python3 -m venv env
```

- Installer les dépendances avec :
```bash
cd /opt/bshell/
source env/bin/activate
pip3 install -r requirements.txt
```

- Ou, installer manuellement les packages nécessaires, par exemple :
```bash
cd /opt/bshell/
source env/bin/activate
pip3 install fastapi flask uvicorn asgiref
```

---

## 🌍 Utilisation du Site Web

### ▶️ Lancement

```bash
cd /opt/bshell/
uvicorn main:app --host 0.0.0.0 --port 8000
```

### 🧑‍💻 Accès

| Élément | URL |
|--------|-----|
| Page d'accueil | http://localhost:8000/ |
| Site Web (liste des CVE) | http://localhost:8000/failles |
| Tableau/States | http://localhost:8000/states |
| Documentation API | http://localhost:8000/docs |
| Liste JSON | http://localhost:8000/v1/cves/ |

---
## 🫖 Utilisation de l'application Java
```bash
bshell [options]
```
| Option              | Description                        |
| ------------------- | ---------------------------------- |
| `--help`            | Affiche l'aide                     |
| `-d`, `--dbPath`    | Chemin vers la base de données     |
| `-D`, `--directory` | Working directory de l'application |

**Remarque :** L'application Java permet de rechercher des vulnérabilités Web, donc pour tester et peupler notre base de données **en toute légalité**, nous pouvons utiliser un docker qui est rempli de CVE Web.

Dans mon cas, j'ai utilisé l'image :
- php:7.4.21-cli (59 vulnérabilités détectées par Nuclei)
- blueteamsteve/cve-2021-41773:no-cgid (10 vulnérabilités détectées par Nuclei)


---

## 🗃️ Base de données

La base SQLite (`dev.db`) contient une table :

| Colonne | Type | Description |
|---------|------|-------------|
| id      | INTEGER (PK) | Identifiant unique |
| name    | TEXT         | Identifiant CVE (ex: CVE-2025-1234) |
| target  | TEXT         | Plateforme / système |
| state   | TEXT         | Statut (open, closed, etc.) |
| infos   | TEXT (JSON)  | Métadonnées JSON |

Exemple d’insertion :

```sql
INSERT INTO cve (name, target, state, infos)
VALUES ('CVE-2025-9999', 'Android', 'open', '{"template-id":"CVE-2014-2323","matched-at":"http://127.0.0.1/etc/passwd","info":%7B"name":"Lighttpd 1.4.34 SQL Injection and Path Traversal","severity":"critical"}}');
```

---

## 🔌 API — Endpoints principaux

| Route | Description |
|------|-------------|
| `GET /v1/cves/` | Liste des CVE Web |
| `GET /v1/cves/?target=Android` | Filtrage |
| `GET /v1/cves/?state=open` | Filtrage |
| `GET /v1/cves/?q=keyword` | Recherche |
| `GET /v1/cves/<id>` | Récupérer une CVE Web spécifique |

---

## 🎯 Objectif

Fournir un **tableau de bord local et autonome** des failles CVE Web, utilisable aussi bien :
- depuis un navigateur web,
- que depuis une application Android (via API).

Fournir une application Java pour scanner les vulnérabilités Web.

---

## 📝 Autres

Dossier visual code : **CLI/BShell/**

---



