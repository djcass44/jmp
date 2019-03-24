# JMP UI

This README details how to setup the JMP UI and connect it the API

If the API has been updated, it is highly recommended to update the UI as they are tightly-coupled and not updating may lead to missing features or potential undefined behaviour.

## Setting up the UI

The JMP UI can be run in 2 different ways.

Edit [.env.production](.env.production)

Set `VUE_APP_BASE_URL` to the URL of the API (e.g. `https://jmp.yourdomain.com/api`)

Set `VUE_APP_FE_URL` to the URL the UI will be running on (e.g. `https://jmp.yourdomain.com`)

1. **Using docker** (recommended)

```bash
docker build -t jmp:2.1 .
docker run jmp:2.1 -p 80:80
```

2. **Standalone**

```bash
sudo npm install -G @vue/cli
npm install
npm run serve
```

The above will serve a non-optimised development build of the UI on `localhost:8080`. In order to serve the optimised build, you will need to serve it manually with a web server. This is done automatically in the [Dockerfile](Dockerfile) using NGINX

To create the optimised files
```bash
npm run build
```
They will be available in the `dist` directory

### Custom branding

The JMP UI currently has limited support for custom branding without editing the source.
In the env file you can set `VUE_APP_APP_NAME` (default: `JMP`) to your custom app title.
