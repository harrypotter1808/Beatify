# MySpotify Real-World Deployment Guide

This guide details how to deploy the full-stack database-driven **MySpotify** application locally or to cloud environments in the real world.

---

## Method 1: Local Deployment with Docker (Easiest)

If you have Docker and Docker Compose installed on your system, you can build and run both the Java server and the MySQL database with a single command:

1. Open your terminal in the `MySpotify` folder.
2. Run the command:
   ```bash
   docker-compose up --build
   ```
3. Docker will automatically:
   - Start a MySQL 8 container listening on host port `3307`.
   - Compile the Java project using Maven inside a container.
   - Run the SparkJava server on port `4567`.
   - Set up the environment connection `jdbc:mysql://mysql:3306/spotify`.
4. Open your browser and navigate to `http://localhost:4567` to start playing music!

---

## Method 2: Deployment to Railway.app (Highly Recommended)

[Railway](https://railway.app/) is a PaaS that makes deploying full-stack Java/database apps incredibly fast and simple.

### Step 1: Push your project to GitHub
Initialize git in your `MySpotify` directory and push it to a repository:
```bash
git init
git add .
git commit -m "Initial commit"
# Push to your GitHub repo
```

### Step 2: Deploy MySQL Database on Railway
1. Log in to Railway and create a new project.
2. Select **Provision MySQL** from the database options.
3. Railway will spin up a MySQL database. Go to its **Variables** tab to find the `MYSQL_URL` or connection string.

### Step 3: Deploy the Java Web Server
1. Click **New** -> **GitHub Repo** and connect your repository.
2. In the deployment settings for the web service, go to **Variables** and add:
   - `PORT` = `4567` (or leave empty, Railway automatically detects the exposed port).
   - `DATABASE_URL` = `${{MYSQL_URL}}` (Railway will automatically inject the connection string for your database container!).
3. Railway will detect the `Dockerfile` in the root and compile/deploy your app automatically.
4. Your application will be live at a public URL (e.g., `https://myspotify-production.up.railway.app`).

---

## Method 3: Deployment to Render.com

[Render](https://render.com/) is another excellent option for hosting web services and databases.

### Step 1: Create a PostgreSQL or MySQL Database
Render natively supports PostgreSQL. Since our server uses JDBC, you can also spin up a MySQL database on a host like [Aiven](https://aiven.io/) or [PlanetScale](https://planetscale.com/), or use Render's built-in PostgreSQL if you modify the driver (our connection pool automatically supports JDBC URLs!).

### Step 2: Create a Web Service on Render
1. Connect your GitHub repository to Render.
2. Choose **Web Service** as the resource type.
3. Select **Docker** as the environment (Render will read your `Dockerfile`).
4. Under **Advanced**, add the environment variable:
   - `DATABASE_URL` = `<your_prod_database_jdbc_url>` (e.g. `jdbc:mysql://host:port/db?user=root&password=pass`)
5. Deploy the service. Your app is live!

---

## Method 4: Deploying to a Cloud VPS (AWS EC2 / DigitalOcean)

If you own a Linux virtual machine (VPS):

1. **Install Docker and Git**:
   ```bash
   sudo apt update
   sudo apt install docker.io docker-compose git -y
   ```
2. **Clone your repository**:
   ```bash
   git clone <your_github_repo_url> MySpotify
   cd MySpotify
   ```
3. **Launch Docker Compose**:
   ```bash
   sudo docker-compose up -d
   ```
4. **Access the application**:
   Open port `4567` in your VPS firewall (AWS Security Group or `ufw`):
   ```bash
   sudo ufw allow 4567/tcp
   ```
   Now visit `http://your-vps-ip:4567`.
