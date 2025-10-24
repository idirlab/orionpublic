# Orion: Interactive Query Builder

Orion is an interactive web-based query builder designed to help users construct complex queries with system-generated suggestions over large knowledge graphs.

---

## 🚀 Clone the Repository

```bash
sudo git clone https://github.com/your-username/orion.git
cd orion
```

*(Replace the above URL with your actual repository link.)*

---

## ⚙️ Compile the Project

### 1. Download Maven
```bash
wget http://mirror.olnevhost.net/pub/apache/maven/binaries/apache-maven-3.2.2-bin.tar.gz
```

### 2. Extract Maven Package
```bash
tar xvf apache-maven-3.2.2-bin.tar.gz
```

### 3. Set Environment Variables
```bash
export MAVEN_HOME=~/apache-maven-3.2.2   # Use absolute path
export M2=$MAVEN_HOME/bin
export PATH=$M2:$PATH
```

### 4. Build Backend
```bash
cd backend
mvn install
```

> **Note:** Run `mvn install` once after project deployment.  
> To recompile after modifying Java files:
> ```bash
> mvn compile
> ```

---

## 🧩 Co-occurrence Set Generation

```bash
cd orion/preprocess-data/training_data
./create_training_data.sh
```

---

## 🧮 UI Data Generation

```bash
cd orion/preprocess-data/ui_data
./preprocess-freebase.sh
```

---

## 🌐 Deploy Orion Web App

### 1. Update Apache Configuration
Edit `/etc/httpd/conf/httpd.conf` and add the following lines at the end:

```
Alias /orion "[full path of ~/orion/frontend]"
<Directory "[full path of /frontend folder]">
   AllowOverride None
   Options None
   allow from all
</Directory>
```

### 2. Configure Proxy Module
Within the `ProxyModule` section of `httpd.conf`, add:

```
ProxyPass /orion_app/ http://localhost:[port_number]/
ProxyPassReverse /orion_app/ http://[ip_address]/orion_app/
Redirect /orion_app http://[ip_address]/orion_app/
```

### 3. Update Frontend Paths
- Edit `/orion/frontend/editor.html`  
  → Find the variable `window.location` and set it to the full path of `~/orion/editor.html`.

- Edit `/orion/frontend/js/app1.js`  
  → Set the variable `urlFull` to your server name.

### 4. Restart Apache Server
```bash
/sbin/service httpd restart
```

### 5. Start Orion Backend Server
```bash
cd orion/backend
nohup ./run_server.sh &
```

### 6. Restart Server
```bash
nohup ./restart_server.sh &
```

---

## 🧪 Run Simulated Experiments

Import partial graphs from:
```
data/orion/data_all/input/testPartialAndTargetQueryFiles-Freebase/
```

Then execute:

```bash
./run_graphTypeQuerySuggestionCompare-freebase-baseline.sh 50 false 5 1 0
./run_graphTypeQuerySuggestionCompare-freebase-rdp.sh 50 false 5 1 0
./run_graphTypeQuerySuggestionCompare-freebase-hybrid.sh 50 false 5 1 0
```

