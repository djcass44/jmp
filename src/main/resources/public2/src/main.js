import Vue from "vue";
import VueRouter from "vue-router";
import "./plugins/vuetify";
import VueClipboard from "vue-clipboard2";
import App from "./App.vue";

import Jumps from "./components/Jumps.vue";
import Users from "./components/Users.vue";
import NotFound from "./components/error/NotFound.vue";
import Token from "./components/Jump/Token.vue";
import Similar from "./components/Jump/Similar.vue";
import Setup from "./components/Jump/Setup.vue";

import SocketIO from "socket.io-client";
import VueSocketIO from "vue-socket.io";
import { BASE_URL } from "./var.js";

Vue.use(VueRouter);
Vue.use(VueClipboard);

Vue.config.productionTip = false;

// Setup Socket.IO
const options = {
    secure: process.env.VUE_APP_SCHEME === "https"
}
Vue.use(new VueSocketIO({
    debug: process.env.NODE_ENV === "development",
    connection: SocketIO("http://localhost:7001", options)
}));
// Disable logging in production
if(process.env.NODE_ENV === "production") {
    console.log = function() {}
}
const router = new VueRouter({
    mode: 'history',
    routes: [
        {
            path: '/',
            component: Jumps
        },
        {
            path: '/settings',
            component: Users
        },
        {
            path: '/setup',
            component: Setup
        },
        {
            path: '/jmp',
            component: Token
        },
        {
            path: '/similar',
            component: Similar
        },
        {
            path: '*',
            component: NotFound
        }
    ]
});

new Vue({
    router,
    render: h => h(App)
}).$mount("#app");
