<template>
    <div id="main-list" v-cloak>
        <v-layout>
            <v-flex xs12 sm6 offset-sm3>
                <p class="subheading"><v-btn flat icon color="grey darken-1" @click="openHome"><v-icon>arrow_back</v-icon></v-btn>Back to home</p>
                <h2 class="display-4 my-4">Setup guide</h2>
                <v-alert class="m2-card" color="info" icon="info" outline value="true">Please be aware that your browser may be updated faster than your version of {{ appName }}.
                    If the below instructions are out of date please bug your local SysAdmin to update the app.
                    You can also view the most recent version of the <a target="_blank" rel="noopener noreferrer" href="https://github.com/djcass44/jmp/blob/develop/README.md#setup">README</a>
                </v-alert>
                <div v-for="item in browsers" :key="item.name">
                    <v-subheader>{{ item.name }}</v-subheader>
                    <v-card class="m2-card">
                        <v-img :src="item.logo" :lazy-src="item.logo" aspect-ratio="5"></v-img>
                        <v-card-title primary-title>
                            <div>
                                <h3 class="headline mb-0">{{ item.name }}</h3>
                                <p v-html="item.content"></p>
                            </div>
                        </v-card-title>
                    </v-card>
                </div>
                <p class="ma-3">Is this page out of date, incorrect or hard to understand?
                    <a target="_blank" rel="noopener noreferrer" href="https://github.com/djcass44/jmp/blob/develop/src/main/resources/public2/src/components/Jump/Setup.vue">Help contribute</a>
                </p>
            </v-flex>
        </v-layout>
    </div>
</template>
<script>
export default {
    name: "Setup",
    data() {
        return {
            browsers: [
                {
                    name: 'Firefox',
                    logo: 'https://upload.wikimedia.org/wikipedia/commons/6/67/Firefox_Logo%2C_2017.svg',
                    content: `1. Add a new bookmark with the following values</br>&emsp;Name = <kbd>${process.env.VUE_APP_APP_NAME}</kbd></br>&emsp;Keyword = <kbd>jmp</kbd></br>&emsp;Location = <kbd>${process.env.VUE_APP_BASE_URL}/jmp?query=%s</kbd>`
                },
                {
                    name: "Google Chrome/Chromium-based browsers",
                    logo: 'https://upload.wikimedia.org/wikipedia/commons/a/a5/Google_Chrome_icon_%28September_2014%29.svg',
                    content: `1. Open settings (<code>chrome://settings</code>)</br>2. Click <b>\'Manage search engines\'</b></br>3. Add new with the following information</br>&emsp;Search engine = <kbd>${process.env.VUE_APP_APP_NAME}</kbd></br>&emsp;Keyword = <kbd>jmp</kbd></br>&emsp;URL = <kbd>${process.env.VUE_APP_BASE_URL}/jmp?query=%s</kbd>`
                },
                {
                    name: 'Safari',
                    logo: 'https://upload.wikimedia.org/wikipedia/commons/5/52/Safari_browser_logo.svg',
                    content: '<kbd>TODO</kbd>'
                }
            ]
        }
    },
    computed: {
        appName: function() {
            return process.env.VUE_APP_APP_NAME;
        }
    },
    mounted: function() {
        this.$emit('postInit');
    },
    methods: {
        setLoggedIn() {},
        authChanged() {},
        loadFailed() {},
        openHome: function(event) {
            window.location.href = process.env.VUE_APP_FE_URL;
        },
    }
};
</script>
<style scoped>
.m2-card {
    border-radius: 12px;
}
</style>
