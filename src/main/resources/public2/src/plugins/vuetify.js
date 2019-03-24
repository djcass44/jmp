import "material-design-icons-iconfont/dist/material-design-icons.css";
import Vue from 'vue';
import Vuetify from 'vuetify/lib';;
import 'vuetify/src/stylus/app.styl';

import colors from "vuetify/es5/util/colors";

Vue.use(Vuetify, {
  iconfont: 'md',
  theme: {
      primary: '#3367d6',
      secondary: colors.blue.darken1,
      accent: colors.pink.base
  }
});
