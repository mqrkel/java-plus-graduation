package ru.practicum.ewm.compilation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.dto.NewCompilationDto;
import ru.practicum.ewm.compilation.dto.UpdateCompilationRequest;
import ru.practicum.ewm.compilation.mapper.CompilationMapper;
import ru.practicum.ewm.compilation.model.Compilation;
import ru.practicum.ewm.compilation.repository.CompilationRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConditionNotMetException;
import ru.practicum.ewm.exception.NotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Сервис для работы с подборками событий (Compilation).
 * Здесь происходит вся магия по поиску и получению подборок из базы.
 * Если подборка не найдена — кидаем NotFoundException, чтобы не оставлять пользователя в подвешенном состоянии.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;

    /**
     * Получить список подборок событий с фильтрацией по признаку "закреплена" и пагинацией.
     *
     * @param pinned фильтр по закреплённости подборок (true/false), или null — без фильтра
     * @param from   количество элементов, которые нужно пропустить (для пагинации)
     * @param size   количество элементов в ответе (для пагинации)
     * @return список DTO подборок событий, может быть пустым, если подходящих нет
     */
    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);

        List<Compilation> compilations = (pinned != null)
                ? compilationRepository.findByPinned(pinned, pageable)
                : compilationRepository.findAll(pageable).getContent();

        return compilations.stream()
                .map(CompilationMapper::toDto)
                .toList();
    }

    /**
     * Получить подборку событий по её ID.
     *
     * @param compId ID подборки
     * @return DTO подборки событий
     * @throws NotFoundException если подборка с таким ID не найдена
     */
    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation", compId));
        return CompilationMapper.toDto(compilation);
    }

    /**
     * Создать новую подборку событий.
     * Проверяет уникальность названия подборки.
     *
     * @param newCompilationDto DTO с данными новой подборки
     * @return DTO созданной подборки
     * @throws ConditionNotMetException если подборка с таким названием уже существует
     */
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        if (compilationRepository.existsByTitle(newCompilationDto.getTitle())) {
            throw new ConditionNotMetException("A compilation with this title already exists");
        }

        Set<Event> events = new HashSet<>();
        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            events = new HashSet<>(eventRepository.findAllById(newCompilationDto.getEvents()));
        }

        Compilation compilation = CompilationMapper.toEntity(newCompilationDto, events);

        Compilation saved = compilationRepository.save(compilation);

        return CompilationMapper.toDto(saved);
    }

    /**
     * Удалить подборку событий по ID.
     *
     * @param compId ID подборки для удаления
     * @throws NotFoundException если подборка с таким ID не найдена
     */
    @Transactional
    @Override
    public void deleteCompilation(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation", compId));
        compilationRepository.delete(compilation);
    }

    /**
     * Обновить информацию о подборке событий.
     * Изменения применяются только к непустым полям запроса.
     *
     * @param compId ID подборки для обновления
     * @param dto    DTO с новыми данными для подборки
     * @return обновленная DTO подборки
     * @throws NotFoundException если подборка с таким ID не найдена
     */
    @Transactional
    @Override
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest dto) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation", compId));

        if (dto.getTitle() != null) {
            compilation.setTitle(dto.getTitle());
        }

        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }

        if (dto.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(dto.getEvents()));
            compilation.setEvents(events);
        }
        return CompilationMapper.toDto(compilation);
    }
}