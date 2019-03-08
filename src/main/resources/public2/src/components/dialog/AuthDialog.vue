<template>
    <div id="auth-dialog" v-cloak>
        <v-layout row justify-center>
            <v-dialog v-model="dialog" persistent max-width="600px">
                <v-form v-model="valid" ref="form">
                    <v-card>
                        <v-card-title>
                            <span class="strong-title headline">{{ title }}</span>
                        </v-card-title>
                        <v-card-text>
                            <v-container grid-list-md>
                                <v-layout wrap>
                                    <v-flex xs12>
                                        <v-text-field outline label="Username*" v-model="name" :rules="nameRules" :counter="36" required autocomplete="username"></v-text-field>
                                    </v-flex>
                                    <v-flex xs12>
                                        <v-text-field outline label="Password*" v-model="password" :rules="passwordRules" :type="'password'" required autocomplete="password"></v-text-field>
                                    </v-flex>
                                </v-layout>
                            </v-container>
                            <small>*indicates required field</small>
                        </v-card-text>
                        <v-card-actions>
                            <v-spacer></v-spacer>
                            <v-btn color="pink" flat @click="dialog = false">Cancel</v-btn>
                            <v-btn color="pink" flat :disabled="!valid" @click="create ? onCreate() : submit()">{{ action }}</v-btn>
                        </v-card-actions>
                    </v-card>
                </v-form>
            </v-dialog>
        </v-layout>
    </div>
</template>
<script>
import axios from "axios";
import { storageUser, storageJWT } from "../../var.js";

const nameRegex= new RegExp('^[a-zA-Z0-9_.-]*$');
export default {
    data () {
        return {
            dialog: false,
            valid: false,
            name: '',
            nameRules: [
                (v) => !!v || 'This is a required field.',
                (v) => nameRegex.test(v) || 'Username must not contain special characters',
                (v) => v && v.length < 37 || 'Username must be less than 37 characters'
            ],
            password: '',
            passwordRules: [
                (v) => !!v || 'This is a required field.'
            ],
            title: 'Authenticate',
            action: 'Login',
            create: false
        }
    },
    created () {
        const vm = this;
        this.$on('dialog-auth', function (value, create) {
            vm.dialog = value;
            vm.$refs.form.reset();
            if(create) {
                vm.create = create;
                vm.title = "Add user";
                vm.action = "Create";
            }
            else {
                vm.create = false;
                vm.title = "Authenticate";
                vm.action = "Login";
            }
        })
    },
    methods: {
        setVisible(visible, create) {
            this.$emit('dialog-auth', visible, create);
        },
        onCreate() {
            this.$refs.form.validate();
            const url = `${process.env.VUE_APP_BASE_URL}/v2/user/add`;
            let that = this;
            let data = window.btoa(`${this.name}:${this.password}`);
            axios.put(
                url,
                {},
                {headers: { 'Authorization': 'Basic ' + data, "Content-Type": "application/json" }}
            ).then(r => {
                that.dialog = false;
                that.$emit('snackbar', true, `Created user ${that.name}`);
                that.$emit("pushItem");
            }).catch(e => {
                console.log(e);
                that.$emit('snackbar', true, `Failed to create user: ${e.response.status}`);
            });
        },
        submit () {
            this.$refs.form.validate();
            const url = `${process.env.VUE_APP_BASE_URL}/v2/user/auth`;
            let that = this;
            let data = window.btoa(`${this.name}:${this.password}`);
            axios.post(
                url,
                {},
                {headers: { 'Authorization': 'Basic ' + data, "Content-Type": "application/json"}}
            ).then(r => {
                that.dialog = false;
                // console.log(r.data);
                localStorage.setItem(storageJWT, r.data);
                localStorage.setItem(storageUser, that.name);
                that.$emit('getAuth');
                that.$emit('pushItem');
            }).catch(function(e) {
                console.log(e);
                if(e.response.status === 404)
                    that.$emit('snackbar', true, "Password incorrect or user doesn't exist");
                else
                    that.$emit('snackbar', true, `Failed to authenticate: ${e.response.status}`);
            });
        }
    }
};
</script>
