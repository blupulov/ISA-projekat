<template>
  <div>
    <h3><strong>BOATS</strong></h3>
    <table class="table table-striped">
      <thead>
      <tr>
        <td>NAME</td>
        <td>FULL ADDRESS</td>
        <td>RATING</td>
      </tr>
      </thead>
      <tbody>
      <tr v-for="boat in boats" :key="boat.id">
        <td>{{boat.name}}</td>
        <td>{{boat.address.street}} {{boat.address.city}} {{boat.address.country}}</td>
        <td>{{boat.rating}}</td>
        <td><button class="btn btn-danger" @click="deleteBoat(boat.id)">DELETE</button></td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import BoatService from "@/Services/BoatService";

export default {
  data(){
    return{
      boats: []
    }
  },

  mounted() {
    this.loadAllBoats();
  },

  methods:{
    loadAllBoats(){
      BoatService.getAllBoats().then(res => {
        this.boats = res.data;
      }).catch(() => {alert("THERE IS SOME PROBLEM WITH LOADING ALL BOATS")});
    },

    deleteBoat(id) {
      BoatService.delete(id).then(() => {this.loadAllBoats()}).catch(() => {
        alert("THERE IS SOME PROBLEM WITH DELETING BOAT");
      })
    }
  }
}
</script>

<style scoped>

</style>