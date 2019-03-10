<template>
    <v-toolbar dark color="primary">
        <!-- <v-toolbar-side-icon></v-toolbar-side-icon> -->
        <img src="assets/ic_launcher.png" width="32" height="32">
        <v-toolbar-title v-ripple @click="openHome" class="white--text">JumpPoints</v-toolbar-title>

        <v-spacer></v-spacer>

        <v-text-field v-if="!nullPage" v-model="searchQuery" @input="textChanged" flat solo light clearable placeholder="Enter a name"></v-text-field>

        <v-spacer></v-spacer>

        <!-- <v-tooltip bottom>
            <template v-slot:activator="{ on }">
                <v-btn icon v-on="on" v-if="!nullPage"><v-icon>search</v-icon></v-btn>
            </template>
            <span>Search</span>
        </v-tooltip> -->

        <v-menu bottom left offset-y origin="top right" transition="scale-transition" v-if="!nullPage" min-width="150">
            <template v-slot:activator="{ on }">
                <v-btn icon v-on="on"><v-icon>account_circle</v-icon></v-btn>
            </template>
            <v-list>
                <v-list-tile v-ripple v-if="!loggedIn" @click="openDialog"><v-list-tile-title>Login</v-list-tile-title></v-list-tile>
                <v-list-tile v-ripple v-if="isAdmin && !slashUsers" @click="openAdmin"><v-list-tile-title>Admin settings</v-list-tile-title></v-list-tile>
                <v-list-tile v-ripple v-if="loggedIn" @click="logout"><v-list-tile-title>Logout</v-list-tile-title></v-list-tile>
            </v-list>
        </v-menu>
    </v-toolbar>
</template>

<script>

export default {
    data () {
        return {
            nullPage: false,
            slashUsers: false,
            searchQuery: '',
            loggedIn: false,
            isAdmin: false
        }
    },
    created() {
        let that = this;
        setTimeout(function() {
            that.$emit('init');
        }, 10);
    },
    methods: {
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
                this.$emit('dialog-create', true, 'New jump point', 'Create')
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
                location.href='/users'
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
            this.$emit('jumpsSetLoggedIn', login);
        }
    }
};
</script>
