import Vue from "vue";
import VueRouter from "vue-router";
import "./plugins/vuetify";
import VueMdl from "vue-mdl";
import App from "./App.vue";

import Jumps from "./components/Jumps.vue";
import Users from "./components/Users.vue";
import NotFound from "./components/error/NotFound.vue";
import Token from "./components/Jump/Token.vue";
import Similar from "./components/Jump/Similar.vue";

Vue.use(VueRouter);
Vue.use(VueMdl);

Vue.config.productionTip = false;

const router = new VueRouter({
    mode: 'history',
    routes: [
        {
            path: '/',
            component: Jumps
        },
        {
            path: '/users',
            component: Users
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
