# Ala-bie-hub

## Setup

Create data directory at `/data/ala-bie-hub` and populate as below (it is easiest to symlink the config files to the ones in this repo):
```
mats@xps-13:/data/ala-bie-hub$ tree
.
└── config
    ├── ala-bie-hub-config.yml -> /home/mats/src/biodiversitydata-se/ala-bie-hub/sbdi/data/config/ala-bie-hub-config.yml
    ├── charts.json -> /home/mats/src/biodiversitydata-se/ala-bie-hub/sbdi/data/config/charts.json
    └── languages.json -> /home/mats/src/biodiversitydata-se/ala-bie-hub/sbdi/data/config/languages.json
```

## Usage

Run locally:
```
make run
```

Build and run in Docker (using Tomcat):
```
make run-docker
```

Make a release. This will create a new tag and push it. A new Docker container will be built on Github.
```
mats@xps-13:~/src/biodiversitydata-se/ala-bie-hub (master *)$ make release

Current version: 1.0.1. Enter the new version (or press Enter for 1.0.2): 
Updating to version 1.0.2
Tag 1.0.2 created and pushed.
```
