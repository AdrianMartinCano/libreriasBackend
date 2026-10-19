package dev.pimon.menu.service;

import dev.pimon.common.exception.AppException;
import dev.pimon.menu.dto.DailyMenuDto;
import dev.pimon.menu.dto.SaveDailyMenuRequest;
import dev.pimon.menu.entity.DailyMenu;
import dev.pimon.menu.entity.DailyMenuCourse;
import dev.pimon.menu.entity.DailyMenuOption;
import dev.pimon.menu.repository.DailyMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyMenuService {

    private final DailyMenuRepository repo;
    private final MenuItemService     itemService;

    /* ── Lecturas ── */

    /** Menús (cualquier estado) de una fecha — vista admin. */
    @Transactional(readOnly = true)
    public List<DailyMenuDto> listForDate(LocalDate date) {
        return repo.findByDateOrderByName(date).stream().map(DailyMenuDto::from).toList();
    }

    /** Menús publicados de una fecha — vista pública. */
    @Transactional(readOnly = true)
    public List<DailyMenuDto> listPublished(LocalDate date) {
        return repo.findByDateAndPublishedTrueOrderByName(date).stream().map(DailyMenuDto::from).toList();
    }

    /** Histórico/calendario admin por rango. */
    @Transactional(readOnly = true)
    public List<DailyMenuDto> listRange(LocalDate from, LocalDate to) {
        return repo.findByDateBetweenOrderByDateDescName(from, to).stream().map(DailyMenuDto::from).toList();
    }

    /** Histórico paginado de todos los menús (más recientes primero) — para revisar menús antiguos. */
    @Transactional(readOnly = true)
    public Page<DailyMenuDto> listPaged(Pageable pageable) {
        return repo.findPage(pageable).map(DailyMenuDto::from);
    }

    @Transactional(readOnly = true)
    public DailyMenuDto findById(String id) {
        return DailyMenuDto.from(findOrThrow(id));
    }

    /* ── Escrituras ── */

    @Transactional
    public DailyMenuDto create(SaveDailyMenuRequest req) {
        LocalDate date = LocalDate.parse(req.date());
        if (repo.existsByDateAndName(date, req.name())) {
            throw AppException.conflict("Ya existe un menú '" + req.name() + "' para esa fecha.");
        }
        DailyMenu d = new DailyMenu();
        d.setDate(date);
        applyContent(d, req);
        return DailyMenuDto.from(repo.save(d));
    }

    @Transactional
    public DailyMenuDto update(String id, SaveDailyMenuRequest req) {
        DailyMenu d = findOrThrow(id);
        LocalDate newDate = LocalDate.parse(req.date());
        boolean sameKey = d.getDate().equals(newDate) && d.getName().equals(req.name());
        if (!sameKey && repo.existsByDateAndName(newDate, req.name())) {
            throw AppException.conflict("Ya existe un menú '" + req.name() + "' para esa fecha.");
        }
        d.setDate(newDate);
        applyContent(d, req);
        return DailyMenuDto.from(repo.save(d));
    }

    @Transactional
    public DailyMenuDto publish(String id) {
        DailyMenu d = findOrThrow(id);
        d.setPublished(true);
        d.setPublishedAt(LocalDateTime.now());
        return DailyMenuDto.from(repo.save(d));
    }

    @Transactional
    public DailyMenuDto unpublish(String id) {
        DailyMenu d = findOrThrow(id);
        d.setPublished(false);
        d.setPublishedAt(null);
        return DailyMenuDto.from(repo.save(d));
    }

    /** Clona un menú a otra fecha (borrador). El caso habitual: "el de ayer con un cambio". */
    @Transactional
    public DailyMenuDto clone(String id, String toDate) {
        DailyMenu src = findOrThrow(id);
        LocalDate target = LocalDate.parse(toDate);
        if (repo.existsByDateAndName(target, src.getName())) {
            throw AppException.conflict("Ya existe un menú '" + src.getName() + "' para esa fecha.");
        }
        DailyMenu copy = new DailyMenu();
        copy.setDate(target);
        copy.setName(src.getName());
        copy.setPrice(src.getPrice());
        copy.setDescription(src.getDescription());
        copy.setPublished(false);
        for (DailyMenuCourse sc : src.getCourses()) {
            DailyMenuCourse cc = new DailyMenuCourse();
            cc.setName(sc.getName());
            cc.setChoose(sc.getChoose());
            for (DailyMenuOption so : sc.getOptions()) {
                DailyMenuOption co = new DailyMenuOption();
                co.setItem(so.getItem());
                co.setName(so.getName());
                co.setDescription(so.getDescription());
                co.setSupplement(so.getSupplement());
                co.setAllergens(new LinkedHashSet<>(so.getAllergens()));
                cc.addOption(co);
            }
            copy.addCourse(cc);
        }
        return DailyMenuDto.from(repo.save(copy));
    }

    @Transactional
    public void delete(String id) {
        repo.delete(findOrThrow(id));
    }

    /* ── Internos ── */

    /** Reemplaza nombre/precio/descripción y reconstruye grupos+opciones (orphanRemoval limpia lo viejo). */
    private void applyContent(DailyMenu d, SaveDailyMenuRequest req) {
        d.setName(req.name());
        d.setPrice(req.price());
        d.setDescription(req.description());
        d.getCourses().clear();
        if (req.courses() == null) return;

        for (SaveDailyMenuRequest.CourseRequest cr : req.courses()) {
            DailyMenuCourse c = new DailyMenuCourse();
            c.setName(cr.name());
            c.setChoose(cr.choose());
            if (cr.options() != null) {
                for (SaveDailyMenuRequest.OptionRequest or : cr.options()) {
                    c.addOption(buildOption(or));
                }
            }
            d.addCourse(c);
        }
    }

    /** Una opción referencia un plato de la carta o es una línea libre (con nombre obligatorio). */
    private DailyMenuOption buildOption(SaveDailyMenuRequest.OptionRequest or) {
        DailyMenuOption o = new DailyMenuOption();
        o.setSupplement(or.supplement());
        if (or.itemId() != null && !or.itemId().isBlank()) {
            o.setItem(itemService.findOrThrow(or.itemId()));   // hereda nombre/desc/alérgenos del plato
        } else {
            if (or.name() == null || or.name().isBlank()) {
                throw AppException.badRequest("Cada opción debe referenciar un plato de la carta o tener un nombre.");
            }
            o.setName(or.name());
            o.setDescription(or.description());
            o.setAllergens(or.allergens() != null ? new LinkedHashSet<>(or.allergens()) : new LinkedHashSet<>());
        }
        return o;
    }

    private DailyMenu findOrThrow(String id) {
        return repo.findById(id).orElseThrow(() -> AppException.notFound("Menú del día no encontrado: " + id));
    }
}
