#ifndef funcionesTienda_h
#define funcionesTienda_h

#include <jansson.h>

typedef struct {
    int socket;
    json_t *carrito;
} S_Cliente;

json_t * leerJSON();
void buscarProducto(const char *nombreProducto, char **respuesta, int tipoBusqueda);
json_t * buscarProductoPor(const char *nombre_producto, json_t *productos, int tipo);

int existencias(json_t *producto, json_t * bd);
int devolverExistencia(const char * id_producto);

void agregarProducto(json_t *carrito, json_t *producto);
void agregarCarrito(S_Cliente *cliente, const char *nombreProducto, char **respuesta);
void editarCarrito(S_Cliente *cliente, const char *idProducto, char **respuesta);
void devolverProductosCarrito(json_t *carrito);

void generarTicket(S_Cliente *cliente, char **respuesta);
json_t *generarDetalleProducto(json_t *producto, double *total_general);

json_t* prepararJSONRespuesta(json_t *resultados);
#endif