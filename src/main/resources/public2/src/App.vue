<template>
    <v-app id="app">
        <!-- Always shows a header, even in smaller screens. -->
        <div>
            <header>
                <Toolbar ref="toolbar"
                    @jumpsSetFilter="jumpsSetFilter"
                    @dialog-create="dialogCreate"
                    @dialog-auth="dialogAuth"
                    @jumpsSetLoggedIn="jumpsSetLoggedIn"
                    @snackbar="snackbar"
                    @loadItems="jumpsLoadItems"
                    @authInvalidate="authInvalidate"
                    @authGet="authGet"
                    @init="toolbarCheckState">
                </Toolbar>
            </header>
            <main>
                <v-content>
                    <Auth ref="auth"
                        @dialog-auth="dialogAuth"
                        @loadFailed="loadFailed"
                        @snackbar="snackbar"
                        @toolbarAuthChanged="toolbarAuthChanged">
                    </Auth>
                    <router-view ref="jumps"
                        @postInit="postInit"
                        @snackbar="snackbar"
                        @dialog-create="dialogCreate"
                        @dialog-delete="dialogDelete">
                    </router-view>
                </v-content>
            </main>
        </div>
        <AuthDialog ref="dialogauth"
            @pushItem="jumpsPushItem"
            @getAuth="authGet"
            @snackbar="snackbar">
        </AuthDialog>
        <Snackbar ref="snackbar"></Snackbar>
    </v-app>
</template>

<script>
import Auth from './components/Auth.vue';

import Jumps from './components/Jumps.vue';
import Users from './components/Users.vue';
import NotFound from './components/error/NotFound.vue';

import Toolbar from './components/Toolbar.vue';

import AuthDialog from './components/dialog/AuthDialog.vue';
import DeleteDialog from './components/dialog/DeleteDialog.vue';

import Snackbar from './components/widget/Snackbar.vue';

import Token from './components/Jump/Token.vue';
import Similar from './components/Jump/Similar.vue';

export default {
    props: ['comp'],
    name: 'App',
    components: {
        Auth,
        Jumps,
        Users,
        Toolbar,
        AuthDialog,
        DeleteDialog,
        Snackbar,
        NotFound,
        Token,
        Similar
    },
    // There has to be a better way!
    methods: {
        dialogAuth(visible, create) {
            this.$refs.dialogauth.setVisible(visible, create);
        },
        dialogCreate(visible, title, action, edit, id, name, location, index) {
            if(this.isSlashUsers())
                this.$refs.dialogauth.setVisible(visible, true);
        },
        dialogDelete(visible, name, index) {
            this.$refs.dialogrm.setVisible(visible, name, index);
        },
        snackbar(visible, text) {
            this.$refs.snackbar.setVisible(visible, text);
        },
        jumpsLoadItems() {
            this.$refs.jumps.loadItems();
        },
        jumpsSetLoggedIn(loggedIn) {
            this.$refs.jumps.setLoggedIn(loggedIn);
        },
        jumpsSetFilter(filter) {
            this.$refs.jumps.setFilter(filter);
        },
        jumpsPushItem(item) {
            this.$refs.jumps.pushItem(item);
        },
        jumpsSetItem(item, index) {
            this.$refs.jumps.setItem(item, index);
        },
        toolbarAuthChanged(login, admin) {
            this.$refs.toolbar.authChanged(login, admin);
            this.$refs.jumps.authChanged(login, admin);
        },
        toolbarCheckState() {
            this.$refs.toolbar.setSlashUsers(!this.isNotSlashUsers());
            this.$refs.toolbar.setNullPage(!this.isNotSlashUsers() && !this.isSlashUsers());
        },
        authGet() {
            this.$refs.auth.getAuth();
        },
        authInvalidate() {
            this.$refs.auth.invalidate();
        },
        loadFailed() {
            this.$refs.jumps.loadFailed();
        },
        isSlashUsers() {
            return this.$refs.jumps.$options.name === "Users";
        },
        isNotSlashUsers() {
            return this.$refs.jumps.$options.name === "Jumps";
        },
        postInit() {
            this.$refs.auth.init();
            this.$refs.toolbar.init();
        }
    }
}
</script>
<style scoped>
html {
    overflow: hidden;
}
main.v-content {
    width: 100vw;
    height: calc(100vh - 64px);
    flex-direction: column;
    overflow: scroll;
    margin-top: 64px;
    padding-top: 0 !important;
}
</style>
