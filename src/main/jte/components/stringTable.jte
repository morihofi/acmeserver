@import de.morihofi.acmeserver.webui.JteLocalizer
@import de.morihofi.acmeserver.webui.compontents.table.TableKey
@import de.morihofi.acmeserver.webui.compontents.table.TableValue
@import java.util.Map

@param JteLocalizer localizer
@param Map<TableKey,TableValue> table


<div class="table-responsive">
    <table class="table table-striped table-hover">
        <thead>
        <tr>
            <th>${localizer.lookup("web.core.table.heading.attribute")}</th>
            <th>${localizer.lookup("web.core.table.heading.value")}</th>
        </tr>
        </thead>
        <tbody>
        @for(TableKey key : table.keySet())
            <tr>
                <td class="icon-text">
                    @if(key.getIconClasses() != null)<i class="${key.getIconClasses()}"></i>&nbsp;@endif${localizer.lookup(key.getValue())}
                </td>
                <td>
                    @if(table.get(key).getValueType() == TableValue.VALUE_TYPE.NONE)
                        <%-- Just the value --%>
                        ${table.get(key).getValue()}
                    @elseif(table.get(key).getValueType() == TableValue.VALUE_TYPE.LINK)
                        <%-- A link with icon --%>
                        <a href="${table.get(key).getValue()}" target="_blank" class="text-decoration-none"><i class="fa-solid fa-link"></i>&nbsp;${table.get(key).getValue()}</a>
                    @endif
                </td>
            </tr>
        @endfor

        </tbody>
    </table>
</div>