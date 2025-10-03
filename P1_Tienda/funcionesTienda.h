#ifndef funcionesTienda_h
#define funcionesTienda_h

#include <jansson.h>



void buscarProducto(const char *nombreProducto, char *respuesta, int tipoBusqueda);
json_t * leerJSON();
json_t* prepararJSONRespuesta(json_t *resultados);
json_t * buscarProductoPor(const char *nombre_producto, json_t *productos, int tipo);

#endif