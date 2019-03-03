import Vue from "vue";
import './plugins/vuetify'
import VueMdl from "vue-mdl";
import App from "./App.vue";

Vue.use(VueMdl);

Vue.config.productionTip = false;

new Vue({
  render: h => h(App)
}).$mount("#app");
