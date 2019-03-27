<template>
    <div id="main-list" v-cloak v-if="init === true">
        <v-subheader inset>Authentication</v-subheader>
        <v-expansion-panel>
            <v-expansion-panel-content>
              <template v-slot:header>
                <div>LDAP (read-only)</div>
              </template>
              <v-card>
                  <v-list two-line subheader>
                    <v-slide-y-transition class="py-0" group>
                        <v-list-tile v-for="i in [1]" :key="i" avatar @click="">
                            <v-list-tile-avatar size="48" class="mx-2" :color="ldap_status === true ? 'green' : 'red'">
                                <v-icon large dark>{{ ldap_status === true ? 'security' : 'error' }}</v-icon>
                            </v-list-tile-avatar>
                            <v-list-tile-content>
                                <v-list-tile-title>LDAP Connected: {{ ldap_status }}</v-list-tile-title>
                                <v-list-tile-sub-title>LDAP is supplying {{ ldap_users }} users</v-list-tile-sub-title>
                            </v-list-tile-content>
                        </v-list-tile>
                    </v-slide-y-transition>
                  </v-list>
                <v-card-text>
                    <v-layout>
                        <v-flex xs12>
                            <div v-for="p in props">
                                <v-switch v-if="typeof p.value === 'boolean'" readonly v-model="p.value" :label="p.name"></v-switch>
                                <v-text-field v-if="typeof p.value !== 'boolean'" readonly :label="p.name" :value="p.value"></v-text-field>
                            </div>
                        </v-flex>
                    </v-layout>
                </v-card-text>
              </v-card>
            </v-expansion-panel-content>
        </v-expansion-panel>
    </div>
</template>
<script>
import axios from "axios";
import { storageJWT } from "../../var.js";

export default {
    name: 'LDAP',
    data() {
        return {
            init: false,
            ldap_status: false,
            ldap_users: 0,
            props: [
                {
                    name: 'ldap',
                    value: ''
                },
                {
                    name: 'ldap.host',
                    value: ''
                },
                {
                    name: 'ldap.port',
                    value: ''
                },
                {
                    name: 'ldap.context',
                    value: ''
                },
                {
                    name: 'ldap.user',
                    value: ''
                },
                {
                    name: 'jmp.ldap.remove_stale',
                    value: ''
                },
                {
                    name: 'jmp.ldap.sync_rate',
                    value: ''
                },
                {
                    name: 'jmp.ldap.user_query',
                    value: ''
                },
                {
                    name: 'jmp.ldap.user_uid',
                    value: ''
                },
                {
                    name: 'jmp.ext.allow_local',
                    value: ''
                }
            ]
        }
    },
    methods: {
        loadProps() {
            this.init = true;
            for(let i = 0; i < this.props.length; i++) {
                axios.get(`${process.env.VUE_APP_BASE_URL}/v2_1/prop/${this.props[i].name}`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(r => {
                    this.props[i].value = r.data;
                }).catch(function(err) {
                    console.log(err);
                    this.props[i].value = 'undefined';
                });
            }
            axios.get(`${process.env.VUE_APP_BASE_URL}/v2_1/provider/main`, { headers: { "Authorization": `Bearer ${localStorage.getItem(storageJWT)}`}}).then(r => {
                this.ldap_status = r.data['connected'];
                this.ldap_users = r.data['users'];
            }).catch(err => {
                console.log(err);
            });
        },
        clear() {
            this.init = false;
        }
    }
};
</script>
