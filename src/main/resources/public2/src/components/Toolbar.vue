<template>
    <v-toolbar absolute dark color="primary">
        <!-- <v-toolbar-side-icon></v-toolbar-side-icon> -->
        <img @click="openHome" src="assets/ic_launcher.png" width="32" height="32" class="mx-2">
        <v-toolbar-title v-ripple @click="openHome" class="white--text hidden-sm-and-down" :style="{ cursor: 'pointer' }">{{ appName }}</v-toolbar-title>

        <v-spacer></v-spacer>

        <v-text-field v-if="!nullPage" v-model="searchQuery" @input="textChanged" single-line flat solo-inverted prepend-inner-icon="search" label="Search"></v-text-field>

        <v-spacer></v-spacer>
        <v-tooltip bottom v-if="nullPage === false && slashUsers === false">
            <template v-slot:activator="{ on }">
                <v-btn icon v-on="on" @click="openSetup"><v-icon>help_outline</v-icon></v-btn>
            </template>
            <span>Setup &amp; help</span>
        </v-tooltip>

        <v-menu bottom left offset-y origin="top right" transition="scale-transition" v-if="!nullPage" min-width="200" :clone-on-content-click="false">
            <template v-slot:activator="{ on }">
                <v-btn icon v-on="on"><v-icon>account_circle</v-icon></v-btn>
            </template>
            <v-card>
                <v-list>
                    <v-list-tile avatar>
                        <v-list-tile-avatar>
                            <v-icon large>{{ getUserAvatar() }}</v-icon>
                        </v-list-tile-avatar>

                        <v-list-tile-content>
                        <v-list-tile-title>{{ getName() }}</v-list-tile-title>
                        <v-list-tile-sub-title>{{ getUserType() }}</v-list-tile-sub-title>
                        </v-list-tile-content>
                    </v-list-tile>
                    <v-list-tile>
                        <v-list-tile-title>{{ version }}</v-list-tile-title>
                    </v-list-tile>
                </v-list>
                <v-divider></v-divider>
                <v-list>
                    <v-list-tile v-if="slashUsers" v-ripple @click="openHome">
                        <v-list-tile-action>
                            <v-icon>home</v-icon>
                        </v-list-tile-action>
                        <v-list-tile-title>Home</v-list-tile-title>
                    </v-list-tile>
                    <v-list-tile v-if="!slashUsers" v-ripple @click="openAdmin">
                        <v-list-tile-action>
                            <v-icon>settings</v-icon>
                        </v-list-tile-action>
                        <v-list-tile-title>Settings</v-list-tile-title>
                    </v-list-tile>
                    <v-list-tile v-if="!loggedIn && allowUserCreation === true" v-ripple @click="openCreateDialog">
                        <v-list-tile-action>
                            <v-icon>person_add</v-icon>
                        </v-list-tile-action>
                        <v-list-tile-title>Create account</v-list-tile-title>
                    </v-list-tile>
                    <v-list-tile v-if="!loggedIn" v-ripple @click="openDialog">
                        <v-list-tile-action>
                            <v-icon>input</v-icon>
                        </v-list-tile-action>
                        <v-list-tile-title>Login</v-list-tile-title>
                    </v-list-tile>
                    <v-list-tile v-if="loggedIn" v-ripple @click="logout">
                        <v-list-tile-action>
                            <v-icon>close</v-icon>
                        </v-list-tile-action>
                        <v-list-tile-title>Logout</v-list-tile-title>
                    </v-list-tile>
                </v-list>
            </v-card>
        </v-menu>
    </v-toolbar>
</template>

<script>
import { storageUser, storageJWT } from "../var.js";
import axios from "axios";

export default {
    data () {
        return {
            nullPage: false,
            slashUsers: false,
            searchQuery: '',
            loggedIn: false,
            isAdmin: false,
            version: '',
            allowUserCreation: true
        }
    },
    computed: {
        appName: function() {
            return process.env.VUE_APP_APP_NAME;
        }
    },
    methods: {
        init() {
            let that = this;
            setTimeout(function() {
                that.$emit('init');
            }, 10);
            axios.get(`${process.env.VUE_APP_BASE_URL}/v2/version`).then(r => {
                that.version = r.data;
            });
        },
        getName: function() {
            let name = localStorage.getItem(storageUser);
            if(name === '' || name == null || this.loggedIn === false) {
                return "Anonymous";
            }
            return name;
        },
        getUserType() {
            if(this.isAdmin === true)
                return "Admin";
            else if(this.loggedIn === true)
                return "User";
            else
                return "Lurker";
        },
        getUserAvatar() {
            let type = this.getUserType();
            if(type === "Admin") return "supervised_user_circle";
            else if(type === "User") return "account_circle";
            else return "person_outline";
        },
        setNullPage(nullPage) {
            console.log(nullPage);
            this.nullPage = nullPage;
        },
        setSlashUsers(slashUsers) {
            this.slashUsers = slashUsers;
        },
        textChanged() {
            this.$emit('jumpsSetFilter', this.searchQuery);
        },
        openJumpDialog: function (event) {
            if(event)
                this.$emit('dialog-create', true, `New ${process.env.VUE_APP_BRAND_NOUN}`, 'Create')
        },
        openDialog: function (event) {
            if(event)
                this.$emit('dialog-auth', true)
        },
        openCreateDialog: function (event) {
            if(event)
                this.$emit('dialog-auth', true, true)
        },
        openAdmin: function (event) {
            if(event)
                location.href='/settings'
        },
        openSetup: function(event) {
            if(event)
                location.href='/setup'
        },
        openHome: function(event) {
            window.location.href = process.env.VUE_APP_FE_URL;
        },
        logout: function (event) {
            if(event) {
                // probably should just reload page
                this.$emit('authInvalidate');
                this.$emit('authGet');
                // this.$emit('loadItems');
            }
        },
        authChanged(login, admin) {
            this.loggedIn = login;
            if(admin) // May not always be true/false
                this.isAdmin = admin;
            else
                this.isAdmin = false;
            this.checkUserCreate();
            this.$emit('jumpsSetLoggedIn', login);
        },
        checkUserCreate() {
            let that = this;
            axios.get(`${process.env.VUE_APP_BASE_URL}/v2_1/uprop/allow_local`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(r => {
                that.allowUserCreation = r.data;
            }).catch(function(err) {
                console.log(err);
                that.allowUserCreation = true;
            });
        }
    }
};
</script>
