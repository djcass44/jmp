<template>
    <div class="mdl-layout__header-row" id="toolbar-overflow">
        <!-- Title -->
        <!--<i class="material-icons">wrap_text</i>-->
        <img src="assets/ic_launcher.png" width="32" height="32">
        <span class="mdl-layout-title strong-title">JumpPoints</span>
        <!-- Add spacer, to align navigation to the right -->
        <div class="mdl-layout-spacer"></div>
        <div class="mdl-textfield mdl-js-textfield mdl-textfield--expandable
              mdl-textfield--floating-label mdl-textfield--align-right">
            <label class="mdl-button mdl-js-button mdl-button--icon"
                   for="fixed-header-drawer-exp">
                <i class="material-icons">search</i>
            </label>
            <div class="mdl-textfield__expandable-holder">
                <input class="mdl-textfield__input" type="text" name="sample"
                       id="fixed-header-drawer-exp" v-model="searchQuery" v-on:input="textChanged">
            </div>
        </div>
        <button v-on:click="openJumpDialog" v-if="loggedIn" class="mdl-button mdl-js-button mdl-button--icon">
            <i class="material-icons">add</i>
        </button>
        <!-- Right aligned menu below button -->
        <button id="auth-button" class="mdl-button mdl-js-button mdl-button--icon">
            <i class="material-icons">more_vert</i>
        </button>
        <ul class="mdl-menu mdl-menu--bottom-right mdl-js-menu mdl-js-ripple-effect" for="auth-button">
            <li v-on:click="openDialog" v-if="!loggedIn" class="mdl-menu__item">Login</li>
            <li v-on:click="openAdmin" v-if="isAdmin" class="mdl-menu__item">Admin settings</li>
            <li v-on:click="logout" v-if="loggedIn" class="mdl-menu__item">Logout</li>
        </ul>
    </div>
</template>

<script>

export default {
    data () {
        return {
            searchQuery: '',
            loggedIn: false,
            isAdmin: false
        }
    },
    methods: {
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
        logout: function (event) {
            if(event) {
                // probably should just reload page
                this.$emit('authInvalidate');
                this.$emit('authGet');
                this.$emit('loadItems');
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
