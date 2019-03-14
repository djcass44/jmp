<template>
    <div id="create-dialog" v-cloak>
        <v-app id="inspire">
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
                                            <v-text-field outline label="Name*" v-model="name" :rules="nameRules" :counter="50" required></v-text-field>
                                        </v-flex>
                                        <v-flex xs12>
                                            <v-text-field outline append-icon="link" label="Location*" v-model="location" :rules="locationRules" :counter="2083" required autocomplete="url"></v-text-field>
                                        </v-flex>
                                        <v-flex xs12>
                                            <v-select v-if="edit === false" v-model="select" :items="items" :rules="typeRules" label="Type" required></v-select>
                                        </v-flex>
                                        <v-flex xs12>
                                            <v-select v-if="edit === false && select === items[2]" v-model="selectGroup" :items="groups" :rules="typeRules" label="Group" required></v-select>
                                        </v-flex>
                                    </v-layout>
                                </v-container>
                                <small>*indicates required field</small>
                            </v-card-text>
                            <v-card-actions>
                                <v-btn color="pink" flat @click="clear">Clear</v-btn>
                                <v-spacer></v-spacer>
                                <v-btn color="pink" flat @click="dialog = false">Cancel</v-btn>
                                <v-btn color="pink" flat :disabled="!valid" @click="edit ? update() : submit()">{{ action }}</v-btn>
                            </v-card-actions>
                        </v-card>
                    </v-form>
                </v-dialog>
            </v-layout>
        </v-app>
    </div>
</template>
<script>
import axios from "axios";
import { storageUser, storageJWT } from "../../var.js";

const urlRegex = new RegExp('https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)');
const nameRegex= new RegExp('^[a-zA-Z0-9_.-]*$');
export default {
    data () {
        return {
            dialog: false,
            valid: false,
            edit: false,
            title: 'New jump point',
            action: 'Create',
            id: -1,
            name: '',
            nameRules: [
                (v) => !!v || 'This is a required field.',
                (v) => nameRegex.test(v) || 'Name must not contain special characters',
                (v) => v && v.length < 50 || 'Name must be less than 50 characters'
            ],
            location: '',
            locationRules: [
                (v) => !!v || 'This is a required field.',
                (v) => urlRegex.test(v) || 'URL must be valid.',
                (v) => v && v.length < 2083 || 'Location must be less than 2083 characters'
            ],
            select: null,
            items: [
                "Global",
                "Personal",
                "Group"
            ],
            typeRules: [
                v => !!v || 'Type is required.'
            ],
            index: -1,
            groupIndex: -1,
            selectGroup: null,
            groups: []
        }
    },
    methods: {
        setVisible(visible, title, action, edit, id, name, location, index) {
            if(edit)
                this.edit = edit;
            else {
                this.edit = false;
                this.$refs.form.reset();
            }
            this.dialog = visible;
            this.title = title;
            this.action = action;
            if(id) {
                this.id = id;
            }
            if(name) {
                this.name = name;
            }
            if(location)
                this.location = location;
            if(index !== undefined)
                this.index = index;
            else
                this.index = -1;
            if(visible === true)
                this.loadGroups();
        },
        loadGroups() {
            let that = this;
            axios.get(`${process.env.VUE_APP_BASE_URL}/v2_1/user/info`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(function(response) {
                let user = response.data;
                return axios.get(`${process.env.VUE_APP_BASE_URL}/v2_1/user/groups?uid=${user.id}`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(function(response) {
                    that.groups = [];
                    response.data.map(item => {
                        that.groups.push(item.name);
                    });
                }).catch(function(error) {
                    console.log(error);
                    that.$emit('snackbar', true, `Failed to load groups: ${error.response.status}`);
                });
            }).catch(function(error) {
                console.log(error);
                that.$emit('snackbar', true, `Failed to load user info: ${error.response.status}`);
            });
        },
        update () {
            this.$refs.form.validate();
            let url = `${process.env.VUE_APP_BASE_URL}/v1/jumps/edit`;
            let that = this;
            axios.patch(
                url,
                `{ "id": ${this.id}, "name": "${this.name}", "location": "${this.location}" }`,
                {headers: {"Content-Type": "application/json", "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}
            ).then(r => {
                that.dialog = false;
                that.$emit('jumpsSetItem', { name: this.name, location: this.location, personal: this.select === this.items[1] }, that.index);
                setTimeout(function() {
                    componentHandler.upgradeDom();
                    componentHandler.upgradeAllRegistered();
                }, 0);
                that.$emit('snackbar', true, `Updated ${that.name}`);
            }).catch(e => {
                console.log(e);
                that.$emit('snackbar', true, `Failed to update: ${e.response.status}`);
            });
        },
        submit () {
            this.$refs.form.validate();
            const localToken = localStorage.getItem(storageJWT);
            let personalJump = this.select === this.items[1];
            let owner = null;
            let url = `${process.env.VUE_APP_BASE_URL}/v1/jumps/add`;
            if(this.select === this.items[2]) {// Owned by group
                owner = this.selectGroup;
                url += `?gname=${owner}`;
                console.log(`Group id: ${owner}`);
            }
            if(localToken === null && personalJump === true) {
                // User cannot create personal tokens if not auth'd
                this.$emit('snackbar', true, "Login to create personal jumps!");
                return;
            }
            let that = this;
            axios.put(
                url,
                `{ "name": "${this.name}", "location": "${this.location}", "personal": "${personalJump}" }`,
                {headers: {"Content-Type": "application/json", "Authorization": `Bearer ${localToken}`}}
            ).then(r => {
                that.dialog = false;
                that.$emit('jumpsPushItem', {
                    name: that.name,
                    location: that.location,
                    personal: personalJump
                });
                setTimeout(function() {
                    componentHandler.upgradeDom();
                    componentHandler.upgradeAllRegistered();
                }, 0);
                that.$emit('snackbar', true, `Added ${that.name}`)
            }).catch(e => {
                console.log(e);
                that.$emit('snackbar', true, `Failed to add: ${e.response.status}`);
            });
        },
        clear () {
            this.name = '';
            this.location = '';
            this.$refs.form.reset();
        }
    }
};
</script>
