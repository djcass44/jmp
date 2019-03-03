<template>
    <div id="app">
        <!-- Always shows a header, even in smaller screens. -->
        <div class="mdl-layout mdl-js-layout mdl-layout--fixed-header">
            <header class="mdl-layout__header">
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
            <main class="mdl-layout__content">
                <!-- <Jumps ref="jumps"
                    @snackbar="snackbar"
                    @dialog-create="dialogCreate"
                    @dialog-delete="dialogDelete">
                </Jumps> -->
                <router-view ref="jumps"
                    @snackbar="snackbar"
                    @dialog-create="dialogCreate"
                    @dialog-delete="dialogDelete">
                </router-view>
                <Auth ref="auth"
                    @dialog-auth="dialogAuth"
                    @toolbarAuthChanged="toolbarAuthChanged">
                </Auth>
            </main>
        </div>
        <AuthDialog ref="dialogauth"
            @loadItems="jumpsLoadItems"
            @getAuth="authGet"
            @snackbar="snackbar">
        </AuthDialog>
        <DeleteDialog ref="dialogrm"
            @jumpsDoRemove="jumpsDoRemove">
        </DeleteDialog>
        <JumpDialog ref="dialogjump"
            @jumpsPushItem="jumpsPushItem"
            @jumpsSetItem="jumpsSetItem"
            @snackbar="snackbar">
        </JumpDialog>
        <Snackbar ref="snackbar"></Snackbar>
    </div>
</template>

<script>
import Auth from './components/Auth.vue';

import Jumps from './components/Jumps.vue';
import Users from './components/Users.vue';

import Toolbar from './components/Toolbar.vue';

import AuthDialog from './components/dialog/AuthDialog.vue';
import DeleteDialog from './components/dialog/DeleteDialog.vue';
import JumpDialog from './components/dialog/JumpDialog.vue';

import Snackbar from './components/widget/Snackbar.vue';

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
        JumpDialog,
        Snackbar
    },
    // There has to be a better way!
    methods: {
        dialogAuth(visible, create) {
            this.$refs.dialogauth.setVisible(visible, create);
        },
        dialogCreate(visible, title, action, edit, id, name, location, index) {
            if(this.isSlashUsers())
                this.$refs.dialogauth.setVisible(visible, true);
            else
                this.$refs.dialogjump.setVisible(visible, title, action, edit, id, name, location, index);
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
        jumpsDoRemove(index) {
            this.$refs.jumps.doRemove(index);
        },
        jumpsPushItem(item) {
            this.$refs.jumps.pushItem(item);
        },
        jumpsSetItem(item, index) {
            this.$refs.jumps.setItem(item, index);
        },
        toolbarAuthChanged(login, admin) {
            this.$refs.toolbar.authChanged(login, admin);
        },
        toolbarCheckState() {
            this.$refs.toolbar.setSlashUsers(this.isSlashUsers());
        },
        authGet() {
            this.$refs.auth.getAuth();
        },
        authInvalidate() {
            this.$refs.auth.invalidate();
        },
        isSlashUsers() {
            return this.$refs.jumps.$options.name === "Users";
        }
    }
}
</script>
