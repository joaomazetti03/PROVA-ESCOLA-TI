package com.example.provacrud.Controller;

import com.example.provacrud.Model.Character;
import com.example.provacrud.Model.MagicIten;
import com.example.provacrud.Model.MagicItenType;
import com.example.provacrud.Service.CharacterService;
import com.example.provacrud.Service.MagicIntenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/magicIten")
@Tag(name = "Magic Iten API", description = "Gerencimento do Magic Iten")
public class MagicItenController {

    @Autowired
    private MagicIntenService magicIntenService;
    @Autowired
    private CharacterService characterService;

    @Operation(description = "Lista todos os itens mágicos")
    @GetMapping
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Retorna lista de itens"),
            @ApiResponse(responseCode = "404", description = "Nenhum item encontrado")
    })
    public ResponseEntity<List<MagicIten>> getAllMagicIten() {
        return ResponseEntity.ok(magicIntenService.listMagicIten());
    }

    @Operation(description = "Busca item mágico por id")
    @GetMapping("/{magicItenId}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Retorna item com o id"),
            @ApiResponse(responseCode = "404", description = "Item não encontrado")
    })
    public ResponseEntity<Optional<MagicIten>> getMagicItenById(@PathVariable Long magicItenId) {
        return ResponseEntity.ok(magicIntenService.getMagicItenById(magicItenId));
    }

    @Operation(description = "Busca item mágico atribuido ao personagem")
    @GetMapping("/character/{characterId}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Retorna item por personagem com o id"),
            @ApiResponse(responseCode = "404", description = "Personagem não encontrado")
    })
    public ResponseEntity<List<MagicIten>> getMagicItenByCharacter(@PathVariable Long characterId) {
        return ResponseEntity.ok(magicIntenService.findByCharacterId(characterId));
    }

    @Operation(description = "Busca amuleto por personagem")
    @GetMapping("/character/{characterId}/amulet")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Retorna amuleto"),
            @ApiResponse(responseCode = "404", description = "Amuleto nao encontrado")
    })
    public ResponseEntity<MagicIten> getAmulet(@PathVariable Long characterId) {
        MagicIten amulet = magicIntenService.findByCharacterId(characterId).stream()
                .filter(i -> i.getMagicItenType() == MagicItenType.AMULETO)
                .findFirst().orElse(null);

        if (amulet == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(amulet);
    }

    @Operation(description = "Cria o item mágico")
    @PostMapping
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item criado com sucesso")
    })
    public ResponseEntity<MagicIten> createMagicIten(@RequestBody MagicIten magicIten) {
        if (magicIten.getAttackMagicIten() == 0 && magicIten.getDefenseMagicIten() == 0) {
            throw new IllegalArgumentException("Item não pode ter força e defesa 0");
        }
        if (magicIten.getAttackMagicIten() > 10 && magicIten.getDefenseMagicIten() > 10) {
            throw new IllegalArgumentException("Força e defesa deve ser no máximo 10");
        }

        if (magicIten.getMagicItenType() == MagicItenType.ARMA && magicIten.getDefenseMagicIten() != 0) {
            throw new IllegalArgumentException("Tipo arma deve ter defesa 0");
        }
        if (magicIten.getMagicItenType() == MagicItenType.ARMADURA && magicIten.getAttackMagicIten() != 0) {
            throw new IllegalArgumentException("Tipo armadura deve ter força 0");
        }

        MagicIten savedMagicIten = magicIntenService.insertMagicIten(magicIten);
        return ResponseEntity.ok(savedMagicIten);
    }

    @Operation(description = "Atribui um item mágico ao personagem")
    @PostMapping("/{magicItenId}/add/{characterId}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item atribuido"),
            @ApiResponse(responseCode = "404", description = "Item ou personagem nao encontrado")
    })
    public ResponseEntity<Void> addItenForCharacter(@PathVariable Long magicItenId, @PathVariable Long characterId) {
        MagicIten magicIten = magicIntenService.getMagicItenById(magicItenId).orElse(null);
        Character character = characterService.getCharacterById(characterId).orElse(null);

        if (magicIten == null || character == null) {
            return ResponseEntity.status(404).build();
        }
        if (magicIten.getMagicItenType() == MagicItenType.ARMA) {
            boolean haveMagicIten = character.getMagicItenList().stream()
                    .anyMatch(i -> i.getMagicItenType() == MagicItenType.AMULETO);
            if (haveMagicIten) {
                return ResponseEntity.status(400).build();
            }
        }

        magicIten.setCharacter(character);
        magicIntenService.saveMagicIten(magicIten);
        return ResponseEntity.status(200).build();
    }

    @Operation(description = "Remove item mágico do personagem")
    @DeleteMapping("/{magicItenId}/remove-character")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Item removido"),
            @ApiResponse(responseCode = "404", description = "Item nao encontrado")
    })
    public ResponseEntity<Void> removeMagicItenFromCharacter(@PathVariable Long magicItenId) {
        MagicIten magicIten = magicIntenService.getMagicItenById(magicItenId).orElse(null);
        if (magicIten == null) {
            return ResponseEntity.status(404).build();
        }

        magicIten.setCharacter(null);
        magicIntenService.saveMagicIten(magicIten);
        return ResponseEntity.status(204).build();
    }

    @Operation(description = "Deleta item mágico")
    @DeleteMapping("{magicItenId}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Item apagado"),
            @ApiResponse(responseCode = "404", description = "Item nao encontrado")
    })
    public void deleteMagicItenById(@PathVariable Long magicItenId) {
        magicIntenService.deleteMagicItenById(magicItenId);
    }
}
