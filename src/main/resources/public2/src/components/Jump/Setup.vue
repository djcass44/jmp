<template>
    <div id="main-list" v-cloak>
        <v-layout>
            <v-flex xs12 sm6 offset-sm3>
                <p class="subheading"><v-btn flat icon color="grey darken-1" @click="openHome"><v-icon>arrow_back</v-icon></v-btn>Back to home</p>
                <h2 class="display-3 my-4 text-xs-center font-weight-light">{{ appName }} Help</h2>
                <v-alert class="m2-card" color="info" icon="info" outline value="true">Please be aware that your browser may be updated faster than your version of {{ appName }}.
                    If the below instructions are out of date please bug your local SysAdmin to update the app.
                    You can also view the most recent version of the <a target="_blank" rel="noopener noreferrer" href="https://github.com/djcass44/jmp/blob/develop/README.md#setup">README</a>
                </v-alert>
                <v-subheader inset>Browser setup</v-subheader>
                <v-expansion-panel v-model="start" expand>
                    <v-expansion-panel-content v-for="item in browsers" :key="item.name">
                        <template v-slot:header>
                            <div class="subheading">{{ item.name }}</div>
                        </template>
                        <v-card>
                            <v-img :src="item.logo" :lazy-src="item.logo" aspect-ratio="5"></v-img>
                            <v-card-title primary-title>
                                <div>
                                    <h3 class="headline py-2">{{ item.name }}</h3>
                                    <p v-html="item.content"></p>
                                </div>
                            </v-card-title>
                        </v-card>
                    </v-expansion-panel-content>
                </v-expansion-panel>
                <v-subheader inset>Usage guide</v-subheader>
                <v-card class="m2-card">
                    <v-card-title primary-title>
                        <div>
                            <h3 class="headline py-2">Using {{ appName }}</h3>
                            <p>
                                1. Navigate to your address bar (<kbd>Ctrl/CMD + L</kbd>)</br>
                                2. Type the keyword (<kbd>{{ keyword }}</kbd>) you set earlier followed by where you want to go</br></br>
                                <b>Example 1</b></br>
                                I want to go to Google, which I have setup as <code>g</code></br>
                                <kbd>{{ keyword }} g</kbd></br></br>
                                <b>Example 2</b></br>
                                I want to go to the Kubernetes GitHub repository, which I have setup as <code>gk8</code></br>
                                <kbd>{{ keyword }} gk8</kbd>
                            </p>
                        </div>
                    </v-card-title>
                </v-card>
                <v-subheader inset>Q&amp;A</v-subheader>
                <v-card class="m2-card">
                    <v-card-title primary-title>
                        <div>
                            <div v-for="item in qna" :key="item.q">
                                <p class="headline black--text">{{ item.q }}</p>
                                <p class="body" v-html="item.a"></p>
                            </div>
                        </div>
                    </v-card-title>
                </v-card>
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
            start: [true],
            keyword: 'jmp',
            browsers: [
                {
                    name: "Google Chrome",
                    logo: 'https://upload.wikimedia.org/wikipedia/commons/a/a5/Google_Chrome_icon_%28September_2014%29.svg',
                    content: `1. Open settings (<code>chrome://settings</code>)</br>2. Click <b>\'Manage search engines\'</b></br>3. Add new with the following information</br>&emsp;Search engine = <kbd>${process.env.VUE_APP_APP_NAME}</kbd></br>&emsp;Keyword = <kbd>jmp</kbd></br>&emsp;URL = <kbd>${process.env.VUE_APP_FE_URL}/jmp?query=%s</kbd>`
                },
                {
                    name: 'Firefox',
                    logo: 'https://upload.wikimedia.org/wikipedia/commons/6/67/Firefox_Logo%2C_2017.svg',
                    content: `1. Add a new bookmark with the following values</br>&emsp;Name = <kbd>${process.env.VUE_APP_APP_NAME}</kbd></br>&emsp;Keyword = <kbd>jmp</kbd></br>&emsp;Location = <kbd>${process.env.VUE_APP_FE_URL}/jmp?query=%s</kbd>`
                },
                {
                    name: 'Safari',
                    logo: 'https://upload.wikimedia.org/wikipedia/commons/5/52/Safari_browser_logo.svg',
                    content: '<kbd>TODO</kbd>'
                }
            ],
            qna: [
                {
                    q: "Why are some URLs red?",
                    a: "HTTP URLs are marked as red to communicate their lack of security. HTTP websites are being phased out all across the internet and your browser probably already shows warnings.</br></br>The red highlight is only a warning and doesn't interfere with your ability to access them."
                },
                {
                    q: "Why can't I access my personal Jumps sometimes?",
                    a: "Your login only persists for a set period of time, once it expires you won't be able to access your personal/group Jumps until you login again."
                },
                {
                    q: "Why can't I create global Jumps?",
                    a: "Creating global Jumps is a priviledge given only to Admins. Contact your local SysAdmin if you need your account upgraded."
                },
                {
                    q: "How do I share a Jump with select users?",
                    a: "Create a Group! When you next create a Jump, set the type to <code>Group</code> and select the Group containing your users.</br>The only people who will be able to use this Jump will be the users in the group."
                },
                {
                    q: "I found a bug",
                    a: "Awesome! Create an <a target=\"_blank\" rel=\"noopener noreferrer\" href=\"https://github.com/djcass44/jmp/issues\">issue</a> and it will be looked  at."
                },
                {
                    q: "What happens to my password?",
                    a: "Security is a very important issue and it\'s certainly not ignored here.</br></br>If using <b>local authentication</b>, your password is hashed &amp; salted then stored in the database. It is only used to verify your login and generated tokens for you to use the app. It is never decrypted again and cannot be seen by anyone.</br></br>If using <b>LDAP</b>, your password is never stored and is only used to be verified against the LDAP server."
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
